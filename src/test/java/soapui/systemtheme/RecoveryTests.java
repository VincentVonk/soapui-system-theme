// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.swing.*;

/** Deliberately fail actual restoration and native-session boundaries, then retry. */
public final class RecoveryTests {
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try { restoration(); } catch (Exception e) { throw new RuntimeException(e); }
            });
            nativeRecovery();
            System.out.println("PASS: " + checks + " failure/retry checks (Swing restoration and isolated native sessions)");
            System.exit(0);
        } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
    private static void restoration() throws Exception {
        UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel());
        JPanel root = new JPanel();
        FaultLabel failing = new FaultLabel();
        JLabel healthy = new JLabel("Still restored");
        Color original = new Color(37, 61, 91);
        failing.setForeground(original); healthy.setForeground(original);
        root.add(failing); root.add(healthy);
        LookAndFeel laf = UIManager.getLookAndFeel();
        ThemeEngine engine = new ThemeEngine();
        engine.setDark(true, root);
        failing.reject = original;
        boolean failed = false;
        try { engine.setDark(false, root); } catch (IllegalStateException expected) { failed = true; }
        check(failed, "Injected restore failure was not surfaced");
        check(!engine.isDark(), "Incomplete restore must not patch new components as dark");
        check(healthy.getForeground().equals(original), "Another component was not restored after one failed");
        check(UIManager.getLookAndFeel() == laf, "Global look-and-feel was not restored");
        failing.reject = null;
        engine.setDark(false, root);
        check(failing.getForeground().equals(original), "Retry lost the original snapshot");
        engine.setDark(true, root); engine.setDark(false, root);
        check(failing.getForeground().equals(original), "Successful retry corrupted the next cycle");

        failing.rejectAny = true;
        failed = false;
        try { engine.setDark(true, root); } catch (IllegalStateException expected) { failed = true; }
        check(failed, "Injected dark application failure was not exercised");
        failing.rejectAny = false;
        engine.setDark(false, root);
        check(failing.getForeground().equals(original), "Rollback after failed dark application lost original colors");
        check(UIManager.getLookAndFeel() == laf, "Rollback after failed dark application lost original look-and-feel");
    }
    private static final class FaultLabel extends JLabel {
        Color reject;
        boolean rejectAny;
        @Override public void setForeground(Color color) {
            if (rejectAny || (reject != null && reject.equals(color))) throw new IllegalStateException("Synthetic paint property failure");
            super.setForeground(color);
        }
    }
    private static void nativeRecovery() throws Exception {
        AtomicInteger starts = new AtomicInteger(), exchanges = new AtomicInteger(), closes = new AtomicInteger();
        WindowsTitleBar title = new WindowsTitleBar(() -> {
            if (starts.incrementAndGet() == 1) throw new IOException("Synthetic startup failure");
            return new WindowsTitleBar.Session() {
                public boolean supported() { return true; }
                public String exchange(String command) { exchanges.incrementAndGet(); return "RESULT|1|0"; }
                public void close() { closes.incrementAndGet(); }
            };
        });
        try {
            title.setDark(true); await(() -> starts.get() == 1, "first native startup");
            Thread.sleep(200);
            for (int i = 0; i < 20; i++) title.refresh();
            Thread.sleep(250);
            check(starts.get() == 1, "Window refresh caused automatic retry storm");
            title.setDark(true);
            await(() -> exchanges.get() == 1, "explicit native retry");
            check(starts.get() == 2, "Explicit theme selection did not retry a transient failure");
            title.setDark(false); await(() -> closes.get() == 1, "native Light cleanup");
            check(exchanges.get() == 2, "Recovered native helper did not restore Light");
        } finally { title.close(); }

        AtomicInteger sessions = new AtomicInteger(), ended = new AtomicInteger(), calls = new AtomicInteger();
        WindowsTitleBar brokenPipe = new WindowsTitleBar(() -> {
            int number = sessions.incrementAndGet();
            return new WindowsTitleBar.Session() {
                public boolean supported() { return true; }
                public String exchange(String command) throws IOException {
                    calls.incrementAndGet();
                    if (number == 1) throw new IOException("Synthetic broken helper pipe");
                    return "RESULT|1|0";
                }
                public void close() { ended.incrementAndGet(); }
            };
        });
        try {
            brokenPipe.setDark(true); await(() -> ended.get() == 1, "failed native exchange cleanup");
            check(sessions.get() == 1, "Failed native session was not closed");
            for (int i = 0; i < 10; i++) brokenPipe.refresh();
            Thread.sleep(250);
            check(sessions.get() == 1, "Broken pipe was retried by window events");
            brokenPipe.setDark(true); await(() -> calls.get() == 2, "new session after broken pipe");
            check(sessions.get() == 2, "Retry reused a broken native session");
        } finally { brokenPipe.close(); }
        await(() -> ended.get() == 2, "recovered native session cleanup");
        check(ended.get() == 2, "Recovered native session leaked");

        AtomicInteger unsupportedStarts = new AtomicInteger();
        WindowsTitleBar unsupported = new WindowsTitleBar(() -> {
            unsupportedStarts.incrementAndGet();
            return new WindowsTitleBar.Session() {
                public boolean supported() { return false; }
                public String exchange(String command) { throw new AssertionError("Unsupported helper was used"); }
                public void close() { }
            };
        });
        try {
            unsupported.setDark(true); await(() -> unsupportedStarts.get() == 1, "unsupported probe");
            Thread.sleep(200);
            unsupported.setDark(false); unsupported.setDark(true); unsupported.refresh();
            Thread.sleep(250);
            check(unsupportedStarts.get() == 1, "Unsupported environment was repeatedly probed");
        } finally { unsupported.close(); }

        java.util.concurrent.CountDownLatch opening = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        AtomicInteger cleanup = new AtomicInteger(), commands = new AtomicInteger();
        WindowsTitleBar closing = new WindowsTitleBar(() -> {
            opening.countDown();
            try { if (!release.await(3, java.util.concurrent.TimeUnit.SECONDS)) throw new IOException("Test release timed out"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
            return new WindowsTitleBar.Session() {
                public boolean supported() { return true; }
                public String exchange(String command) { commands.incrementAndGet(); return "RESULT|1|0"; }
                public void close() { cleanup.incrementAndGet(); }
            };
        });
        try {
            closing.setDark(true);
            check(opening.await(3, java.util.concurrent.TimeUnit.SECONDS), "Native opening was not reached");
            closing.close(); release.countDown();
            await(() -> cleanup.get() == 1, "close during helper startup");
            check(commands.get() == 0, "Closed native session applied a stale theme");
        } finally { release.countDown(); closing.close(); }
    }
    private static void await(BooleanSupplier condition, String description) throws Exception {
        long deadline = System.nanoTime() + 4_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Timed out: " + description);
    }
}
