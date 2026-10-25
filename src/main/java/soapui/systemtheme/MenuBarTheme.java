// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.*;

/** Paint-only overlay: SoapUI retains its original menu bar, menus and mouse targets. */
final class MenuBarTheme {
    private final Map<JMenuBar, java.lang.ref.WeakReference<Surface>> surfaces = new WeakHashMap<>();

    void install(JFrame frame) {
        JMenuBar bar = frame.getJMenuBar();
        if (bar == null) return;
        var known = surfaces.get(bar);
        if (known != null && known.get() != null) return;
        Surface surface = new Surface(bar, frame.getLayeredPane());
        surfaces.put(bar, new java.lang.ref.WeakReference<>(surface));
        frame.getLayeredPane().add(surface, JLayeredPane.DEFAULT_LAYER);
        bar.addComponentListener(surface.boundsListener);
        surface.syncBounds();
    }

    void restore() {
        for (var entry : new java.util.ArrayList<>(surfaces.entrySet())) {
            JMenuBar bar = entry.getKey();
            Surface surface = entry.getValue().get();
            if (surface == null) { surfaces.remove(bar); continue; }
            bar.removeComponentListener(surface.boundsListener);
            Container parent = surface.getParent();
            if (parent != null) { parent.remove(surface); parent.repaint(); }
            surfaces.remove(bar);
        }
    }

    private static final class Surface extends JComponent {
        private final java.lang.ref.WeakReference<JMenuBar> reference;
        private final JLayeredPane pane;
        final ComponentAdapter boundsListener = new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) { syncBounds(); }
            @Override public void componentResized(ComponentEvent e) { syncBounds(); }
            @Override public void componentShown(ComponentEvent e) { syncBounds(); }
            @Override public void componentHidden(ComponentEvent e) { syncBounds(); }
        };
        Surface(JMenuBar bar, JLayeredPane pane) {
            reference = new java.lang.ref.WeakReference<>(bar);
            this.pane = pane;
            setFocusable(false);
            setOpaque(false);
        }
        void syncBounds() {
            JMenuBar bar = reference.get();
            if (bar == null || bar.getParent() == null) { setVisible(false); return; }
            setBounds(SwingUtilities.convertRectangle(bar.getParent(), bar.getBounds(), pane));
            setVisible(bar.isVisible());
            repaint();
        }
        @Override public boolean contains(int x, int y) { return false; }
        @Override protected void paintComponent(Graphics g) {
            JMenuBar bar = reference.get();
            if (bar == null) return;
            g.setColor(ThemeEngine.BG); g.fillRect(0, 0, getWidth(), getHeight());
            if (bar.getBorder() != null) bar.getBorder().paintBorder(bar, g, 0, 0, getWidth(), getHeight());
            for (Component child : bar.getComponents()) if (child.isVisible()) {
                Graphics copy = g.create(child.getX(), child.getY(), child.getWidth(), child.getHeight());
                try { child.printAll(copy); } finally { copy.dispose(); }
            }
            g.setColor(ThemeEngine.LINE); g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }
}
