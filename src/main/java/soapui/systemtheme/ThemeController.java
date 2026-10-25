// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

final class ThemeController {
    enum Mode { SYSTEM, LIGHT, DARK }
    private static final Logger LOG = Logger.getLogger("SoapUI.SystemTheme");
    private final JFrame frame;
    private final ThemeEngine engine = new ThemeEngine();
    private final WindowsTitleBar titleBars = new WindowsTitleBar();
    @FunctionalInterface interface ThemeReader { Optional<Boolean> read() throws Exception; }
    private final ThemeReader reader;
    private final Path settings;
    private final long pollMillis;
    private final Map<Mode, JRadioButtonMenuItem> buttons = new EnumMap<>(Mode.class);
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "soapui-system-theme-reader"); t.setDaemon(true); return t;
    });
    private Mode mode = Mode.SYSTEM;
    private Boolean windowsDark;
    private boolean closed;
    private boolean warned;
    private final java.util.Set<Component> pending = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private boolean patchQueued;
    private final AWTEventListener uiListener = this::onUiEvent;

    ThemeController(JFrame frame) {
        this(frame, new WindowsAppTheme()::read,
            Path.of(System.getProperty("user.home"), ".soapuios", "system-theme.properties"), 3000);
    }
    ThemeController(JFrame frame, ThemeReader reader, Path settings, long pollMillis) {
        this.frame = frame; this.reader = reader; this.settings = settings; this.pollMillis = pollMillis;
    }

    void start() {
        loadMode();
        JMenuBar bar = frame.getJMenuBar();
        if (bar != null) bar.add(buildMenu());
        Toolkit.getDefaultToolkit().addAWTEventListener(uiListener,
            AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { close(); }
        });
        frame.revalidate();
        applyMode();
        worker.scheduleWithFixedDelay(this::poll, 0, pollMillis, TimeUnit.MILLISECONDS);
        LOG.info("System Theme 0.3.2 started; mode=" + mode + "; checks Windows app mode every 3 seconds");
    }

    private JMenu buildMenu() {
        JMenu menu = new JMenu("Theme");
        ButtonGroup group = new ButtonGroup();
        for (Mode value : Mode.values()) {
            String label = value == Mode.SYSTEM ? "Follow Windows" : value == Mode.LIGHT ? "Light (SoapUI default)" : "Dark";
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, mode == value);
            item.addActionListener(e -> { mode = value; saveMode(); applyMode(); });
            buttons.put(value, item); group.add(item); menu.add(item);
        }
        menu.addSeparator();
        menu.add("About System Theme").addActionListener(e -> JOptionPane.showMessageDialog(frame,
            "System Theme 0.3.2 for SoapUI Open Source 5.10.0\n\n"
            + "Follow Windows reads the Windows app mode every 3 seconds.\n"
            + "Your Windows scheduler controls the switch time.\n"
            + "Light restores the original SoapUI look and component colours.\n\n"
            + "No requests, credentials, projects, or Windows settings are modified.\n"
            + "SoapUI start pages and Endpoint Explorer receive a separate dark stylesheet.\n"
            + "Windows 11 native title bars follow the theme through an optional local helper.\n"
            + "Other web pages, some legacy icons, charts and native system dialogs retain their own styling.",
            "System Theme", JOptionPane.INFORMATION_MESSAGE));
        return menu;
    }

    private void poll() {
        try {
            Optional<Boolean> value = reader.read();
            if (value.isPresent()) {
                warned = false;
                SwingUtilities.invokeLater(() -> {
                    if (closed) return;
                    boolean changed = windowsDark == null || !windowsDark.equals(value.get());
                    windowsDark = value.get();
                    if (changed && mode == Mode.SYSTEM) applyMode();
                });
            } else warnOnce("Windows app-mode preference could not be read; retaining current appearance", null);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (Exception e) { warnOnce("Windows theme query failed; retaining current appearance", e); }
    }

    private void warnOnce(String message, Exception cause) {
        if (!warned) { LOG.log(Level.WARNING, message, cause); warned = true; }
    }

    private void applyMode() {
        boolean dark = mode == Mode.DARK || (mode == Mode.SYSTEM && Boolean.TRUE.equals(windowsDark));
        try { engine.setDark(dark, Window.getWindows()); titleBars.setDark(dark); }
        catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not change appearance; restoring light mode", e);
            try { engine.setDark(false, Window.getWindows()); titleBars.setDark(false); }
            catch (Exception rollback) { LOG.log(Level.SEVERE, "Appearance restoration failed", rollback); }
        }
    }

    private void onUiEvent(AWTEvent event) {
        if (closed || !engine.isDark() || engine.isApplying()) return;
        Component component = null;
        if (event instanceof ContainerEvent && event.getID() == ContainerEvent.COMPONENT_ADDED)
            component = ((ContainerEvent) event).getChild();
        else if (event instanceof WindowEvent && event.getID() == WindowEvent.WINDOW_OPENED)
            component = ((WindowEvent) event).getWindow();
        if (component == null) return;
        if (component instanceof Window) titleBars.refresh();
        pending.add(component);
        if (patchQueued) return;
        patchQueued = true;
        SwingUtilities.invokeLater(() -> {
            patchQueued = false;
            Component[] additions = pending.toArray(new Component[0]); pending.clear();
            if (closed || !engine.isDark()) return;
            for (Component addition : additions) {
                try { engine.patchTree(addition); }
                catch (RuntimeException e) { LOG.log(Level.WARNING, "Could not theme a newly opened component", e); }
            }
        });
    }

    private void loadMode() {
        if (!Files.isRegularFile(settings)) return;
        try (InputStream in = Files.newInputStream(settings)) {
            Properties p = new Properties(); p.load(in);
            mode = Mode.valueOf(p.getProperty("mode", "SYSTEM").trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) { LOG.log(Level.WARNING, "Invalid theme setting; using Follow Windows", e); mode = Mode.SYSTEM; }
    }

    private void saveMode() {
        try {
            Files.createDirectories(settings.getParent());
            Properties p = new Properties(); p.setProperty("mode", mode.name());
            try (OutputStream out = Files.newOutputStream(settings)) { p.store(out, "SoapUI System Theme; SYSTEM, LIGHT or DARK"); }
        } catch (Exception e) { LOG.log(Level.WARNING, "Theme choice applies for this session but could not be saved", e); }
    }

    private void close() {
        closed = true; worker.shutdownNow(); pending.clear();
        engine.close();
        titleBars.close();
        Toolkit.getDefaultToolkit().removeAWTEventListener(uiListener);
    }
}
