// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Real DWM read-back, controller integration, new dialogs and compositor screenshots. */
public final class WindowsTitleBarTests {
    private static JFrame frame;
    private static JDialog dialog;
    private static JTextField field;
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        Thread watchdog = new Thread(() -> {
            try { Thread.sleep(45000); } catch (InterruptedException e) { return; }
            System.err.println("Native title-bar test timed out"); System.exit(2);
        }, "native-titlebar-test-watchdog");
        watchdog.setDaemon(true); watchdog.start();
        try { run(Path.of(args[0])); System.exit(0); }
        catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
    private static void run(Path output) throws Exception {
        Files.createDirectories(output);
        Files.deleteIfExists(output.resolve("supported"));
        Path settings = output.resolve("settings.properties"); Files.deleteIfExists(settings);
        try (WindowsTitleBar.NativeBridge inspector = new WindowsTitleBar.NativeBridge()) {
            if (!inspector.supported) {
                System.out.println("SKIP: native title-bar integration requires Windows 11 build 22000 or later."); return;
            }
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("System Theme native title-bar test");
                frame.setFocusableWindowState(false); frame.setAutoRequestFocus(false);
                frame.setLocation(-10000,-10000); frame.setSize(680,240);
                JMenuBar menu = new JMenuBar(); menu.add(new JMenu("File")); frame.setJMenuBar(menu);
                field = new JTextField("Unsaved request text stays intact"); frame.add(field);
                frame.setVisible(true);
                new ThemeController(frame, () -> Optional.of(false), settings, 1000).start();
            });
            Map<Long,List<Long>> original = inspect(inspector);
            check(original.size() == 1, "Expected one decorated native fixture frame: " + original);
            for (int cycle=0; cycle<3; cycle++) {
                choose(2);
                waitFor(() -> dark(inspectUnchecked(inspector)), "dark native caption read-back");
                check(!frame.isUndecorated(), "Native window decorations were replaced");
                if (cycle == 0) {
                    capture(output.resolve("native-titlebar-dark.png"), true);
                    SwingUtilities.invokeAndWait(() -> {
                        dialog = new JDialog(frame, "Opened in dark mode", false);
                        dialog.setFocusableWindowState(false); dialog.setAutoRequestFocus(false);
                        dialog.setLocation(-10000,-10000); dialog.setSize(400,180); dialog.addNotify();
                    });
                    Map<Long,List<Long>> beforeOpening = inspect(inspector);
                    for (Map.Entry<Long,List<Long>> entry : beforeOpening.entrySet())
                        if (!original.containsKey(entry.getKey())) original.put(entry.getKey(), entry.getValue());
                    check(original.size() == 2, "New dialog baseline was not captured");
                    SwingUtilities.invokeAndWait(() -> dialog.setVisible(true));
                    waitFor(() -> dark(inspectUnchecked(inspector)), "new dialog native caption");
                }
                choose(1);
                waitFor(() -> original.equals(inspectUnchecked(inspector)), "exact native attribute restoration");
                SwingUtilities.invokeAndWait(() -> check(field.getText().equals("Unsaved request text stays intact"), "Document text changed"));
            }
            capture(output.resolve("native-titlebar-restored.png"), false);
            // Closing a pipe also restores changes; no application-wide setting is needed.
            try (WindowsTitleBar.NativeBridge session = new WindowsTitleBar.NativeBridge()) {
                check(session.exchange("DARK").endsWith("|0"), "Native helper reported a write failure");
                check(dark(inspect(inspector)), "Helper did not apply actual DWM colors");
            }
            check(original.equals(inspect(inspector)), "EOF did not restore original native attributes");
            SwingUtilities.invokeAndWait(() -> { dialog.dispose(); frame.dispose(); });
            waitFor(() -> ProcessHandle.current().descendants().filter(p -> p.info().command().orElse("").toLowerCase(Locale.ROOT).endsWith("powershell.exe")).count() <= 1,
                "controller helper shutdown");
        }
        Files.writeString(output.resolve("supported"), "Windows 11 DWM title bars validated\n");
        System.out.println("PASS: " + checks + " native title-bar checks (three cycles, new dialog, read-back, EOF cleanup, real compositor screenshots)");
    }
    private static void choose(int index) throws Exception {
        SwingUtilities.invokeAndWait(() -> frame.getJMenuBar().getMenu(1).getItem(index).doClick());
    }
    private static Map<Long,List<Long>> inspect(WindowsTitleBar.NativeBridge inspector) throws Exception {
        String response = inspector.exchange("INSPECT");
        if (!response.startsWith("STATE|")) throw new AssertionError(response);
        Map<Long,List<Long>> states = new TreeMap<>();
        for (String record : response.substring(6).split(";")) {
            if (record.isEmpty()) continue;
            String[] values = record.split(",");
            states.put(Long.parseLong(values[0]), List.of(Long.parseLong(values[1])));
        }
        return states;
    }
    private static Map<Long,List<Long>> inspectUnchecked(WindowsTitleBar.NativeBridge inspector) {
        try { return inspect(inspector); } catch (Exception e) { throw new RuntimeException(e); }
    }
    private static boolean dark(Map<Long,List<Long>> states) {
        return !states.isEmpty() && states.values().stream().allMatch(value -> value.equals(List.of(1L)));
    }
    private static void waitFor(BooleanSupplier condition, String description) throws Exception {
        long until = System.nanoTime() + 12_000_000_000L;
        while (System.nanoTime() < until) {
            if (condition.getAsBoolean()) { checks++; return; }
            Thread.sleep(80);
        }
        throw new AssertionError("Timed out: " + description);
    }
    private static void capture(Path path, boolean expectDark) throws Exception {
        Rectangle[] bounds = new Rectangle[1];
        SwingUtilities.invokeAndWait(() -> {
            frame.setAlwaysOnTop(true); frame.setLocation(60,60); bounds[0] = frame.getBounds();
        });
        // printAll does not capture DWM's real title bar. Capture only this test window.
        Robot robot = new Robot(); robot.delay(350);
        BufferedImage image = robot.createScreenCapture(bounds[0]);
        ImageIO.write(image, "png", path.toFile());
        if (expectDark) {
            Color caption = new Color(image.getRGB(400,15));
            check(caption.getRed()<80 && caption.getGreen()<80 && caption.getBlue()<80, "Real composited title bar is still bright: " + caption);
        }
        SwingUtilities.invokeAndWait(() -> { frame.setAlwaysOnTop(false); frame.setLocation(-10000,-10000); });
    }
}
