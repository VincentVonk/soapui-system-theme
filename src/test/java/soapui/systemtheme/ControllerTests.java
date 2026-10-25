// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import javax.swing.text.Document;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/** Drives the real asynchronous controller with simulated OS readings, without changing Windows. */
public final class ControllerTests {
    private static JFrame frame;
    private static RSyntaxTextArea editor;
    private static JTextField added;
    private static Document originalDocument;
    private static String originalText;
    private static final java.util.concurrent.atomic.AtomicInteger documentChanges = new java.util.concurrent.atomic.AtomicInteger();
    private static JMenuItem item(int index) {
        JMenuBar bar = frame.getJMenuBar();
        return bar.getMenu(bar.getMenuCount()-1).getItem(index);
    }
    public static void main(String[] args) {
        try { run(Path.of(args[0])); System.exit(0); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    private static void run(Path directory) throws Exception {
        Path settings = directory.resolve("controller-test.properties"); Files.deleteIfExists(settings);
        AtomicReference<Optional<Boolean>> preference = new AtomicReference<>(Optional.of(false));
        SwingUtilities.invokeAndWait(() -> {
            try { UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel()); }
            catch (Exception e) { throw new RuntimeException(e); }
            frame = new JFrame("System Theme controller test");
            frame.setFocusableWindowState(false); frame.setAutoRequestFocus(false);
            JMenuBar bar = new JMenuBar(); bar.add(new JMenu("File")); frame.setJMenuBar(bar);
            editor = new RSyntaxTextArea(); editor.setText("An unsaved request"); originalDocument = editor.getDocument();
            frame.add(editor, BorderLayout.CENTER); frame.setSize(600,300); frame.setLocation(-10000,-10000); frame.setVisible(true);
            originalText = editor.getText();
            originalDocument.addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { documentChanges.incrementAndGet(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { documentChanges.incrementAndGet(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { documentChanges.incrementAndGet(); }
            });
            new ThemeController(frame, preference::get, settings, 75).start();
        });
        waitFor(() -> !editor.getBackground().equals(ThemeEngine.INPUT), "initial light");
        preference.set(Optional.of(true));
        waitFor(() -> editor.getBackground().equals(ThemeEngine.INPUT), "automatic dark switch");
        SwingUtilities.invokeAndWait(() -> {
            added = new JTextField("Opened after switching"); frame.add(added, BorderLayout.SOUTH); frame.validate();
        });
        waitFor(() -> added.getBackground().equals(ThemeEngine.INPUT), "new component event");
        preference.set(Optional.empty());
        Thread.sleep(180);
        waitFor(() -> editor.getBackground().equals(ThemeEngine.INPUT), "failed read keeps dark");
        preference.set(Optional.of(false));
        waitFor(() -> !editor.getBackground().equals(ThemeEngine.INPUT), "automatic light switch");
        preference.set(Optional.of(true));
        waitFor(() -> editor.getBackground().equals(ThemeEngine.INPUT), "second automatic dark switch");
        SwingUtilities.invokeAndWait(() -> item(1).doClick());
        waitFor(() -> !editor.getBackground().equals(ThemeEngine.INPUT), "manual light override");
        preference.set(Optional.of(false)); Thread.sleep(120); preference.set(Optional.of(true)); Thread.sleep(180);
        waitFor(() -> !editor.getBackground().equals(ThemeEngine.INPUT), "manual mode survives OS changes");
        if (!Files.readString(settings).contains("mode=LIGHT")) throw new AssertionError("preference not saved");
        SwingUtilities.invokeAndWait(() -> item(0).doClick());
        waitFor(() -> editor.getBackground().equals(ThemeEngine.INPUT), "return to Follow Windows");
        SwingUtilities.invokeAndWait(() -> {
            if (editor.getDocument() != originalDocument || !editor.getText().equals(originalText) || documentChanges.get() != 0)
                throw new AssertionError("controller changed document: identity=" + (editor.getDocument() == originalDocument)
                    + "; text=[" + editor.getText() + "]");
            item(1).doClick(); frame.dispose();
        });
        System.out.println("PASS: asynchronous light/dark/light switching, read failure, new components, menu overrides, preference persistence and unchanged document");
    }
    private static void waitFor(BooleanSupplier condition, String description) throws Exception {
        long deadline = System.nanoTime() + 4_000_000_000L;
        while (System.nanoTime() < deadline) {
            boolean[] ok = {false}; SwingUtilities.invokeAndWait(() -> ok[0] = condition.getAsBoolean());
            if (ok[0]) return;
            Thread.sleep(25);
        }
        throw new AssertionError("Timed out: " + description);
    }
}
