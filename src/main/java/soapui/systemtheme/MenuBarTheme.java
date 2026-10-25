// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.*;
import javax.swing.plaf.LayerUI;

/** A painting origin for the original menu bar, including descendant/hover repaints. */
final class MenuBarTheme {
    private final Map<JRootPane, WeakReference<MenuLayout>> layouts = new WeakHashMap<>();

    void install(JFrame frame) {
        JRootPane root = frame.getRootPane();
        JMenuBar bar = root.getJMenuBar();
        if (bar == null) return;
        var known = layouts.get(root);
        if (known != null && known.get() != null) return;
        MenuLayout layout = new MenuLayout(root, bar);
        layouts.put(root, new WeakReference<>(layout));
        layout.install();
    }

    void restore() {
        for (var entry : new java.util.ArrayList<>(layouts.entrySet())) {
            MenuLayout layout = entry.getValue().get();
            if (layout != null) layout.restore();
            layouts.remove(entry.getKey());
        }
    }

    /** RootLayout still measures the original JMenuBar; transfer its bounds to the paint layer. */
    private static final class MenuLayout implements LayoutManager2 {
        final JRootPane root;
        final JMenuBar bar;
        final LayoutManager original;
        final JLayer<JMenuBar> layer;
        final int level, position;
        MenuLayout(JRootPane root, JMenuBar bar) {
            this.root = root; this.bar = bar; original = root.getLayout();
            JLayeredPane pane = root.getLayeredPane();
            level = pane.getLayer(bar); position = pane.getPosition(bar);
            layer = new JLayer<>(null, new MenuPaint());
        }
        void install() {
            root.setLayout(this);
            // JRootPane.getJMenuBar() and SoapUI.getMenuBar() continue to return the same bar.
            layer.setView(bar);
            root.getLayeredPane().add(layer, Integer.valueOf(level), Math.max(0,position));
            root.revalidate(); root.repaint();
            layoutContainer(root);
        }
        void restore() {
            if (root.getLayout() == this) root.setLayout(original);
            if (bar.getParent() == layer) {
                layer.setView(null);
                if (root.getJMenuBar() == bar)
                    root.getLayeredPane().add(bar, Integer.valueOf(level), Math.max(0,position));
            }
            Container parent = layer.getParent();
            if (parent != null) parent.remove(layer);
            root.revalidate(); root.repaint();
        }
        @Override public void layoutContainer(Container parent) {
            original.layoutContainer(parent);
            if (root.getJMenuBar() != bar || bar.getParent() != layer) return;
            // The delegate assigns layered-pane coordinates to the bar. The layer takes those
            // coordinates and its view is then laid out locally, preserving root-pane geometry.
            layer.setBounds(bar.getBounds());
            layer.setVisible(bar.isVisible());
            layer.doLayout();
        }
        @Override public Dimension preferredLayoutSize(Container parent) { return original.preferredLayoutSize(parent); }
        @Override public Dimension minimumLayoutSize(Container parent) { return original.minimumLayoutSize(parent); }
        @Override public Dimension maximumLayoutSize(Container parent) {
            return original instanceof LayoutManager2 ? ((LayoutManager2)original).maximumLayoutSize(parent) : new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE);
        }
        @Override public float getLayoutAlignmentX(Container parent) { return original instanceof LayoutManager2 ? ((LayoutManager2)original).getLayoutAlignmentX(parent) : .5f; }
        @Override public float getLayoutAlignmentY(Container parent) { return original instanceof LayoutManager2 ? ((LayoutManager2)original).getLayoutAlignmentY(parent) : .5f; }
        @Override public void invalidateLayout(Container parent) { if (original instanceof LayoutManager2) ((LayoutManager2)original).invalidateLayout(parent); }
        @Override public void addLayoutComponent(Component c, Object constraints) { if (original instanceof LayoutManager2) ((LayoutManager2)original).addLayoutComponent(c,constraints); }
        @Override public void addLayoutComponent(String name, Component c) { original.addLayoutComponent(name,c); }
        @Override public void removeLayoutComponent(Component c) { original.removeLayoutComponent(c); }
    }

    /** JLayer routes partial repaint operations here instead of underneath a sibling overlay. */
    private static final class MenuPaint extends LayerUI<JMenuBar> {
        @Override public void paint(Graphics graphics, JComponent component) {
            Component view = ((JLayer<?>)component).getView();
            if (!(view instanceof JMenuBar)) return;
            JMenuBar bar = (JMenuBar)view;
            Graphics g = graphics.create(bar.getX(),bar.getY(),bar.getWidth(),bar.getHeight());
            try {
                g.setColor(ThemeEngine.BG); g.fillRect(0,0,bar.getWidth(),bar.getHeight());
                if (bar.getBorder() != null) bar.getBorder().paintBorder(bar,g,0,0,bar.getWidth(),bar.getHeight());
                for (Component child : bar.getComponents()) if (child.isVisible()) {
                    Graphics copy = g.create(child.getX(),child.getY(),child.getWidth(),child.getHeight());
                    try { child.printAll(copy); } finally { copy.dispose(); }
                }
                g.setColor(ThemeEngine.LINE); g.drawLine(0,bar.getHeight()-1,bar.getWidth(),bar.getHeight()-1);
            } finally { g.dispose(); }
        }
    }
}
