// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.*;
import javax.swing.plaf.LayerUI;

/** Covers SoapUI's hardcoded white toolbar paint while keeping the actual toolbar and all actions alive. */
final class ToolbarPaintGuard {
    private final Map<JComponent, WeakReference<JLayer<JComponent>>> layers = new WeakHashMap<>();

    void install(JComponent component) {
        WeakReference<JLayer<JComponent>> known = layers.get(component);
        if (known != null && known.get() != null && component.getParent() == known.get()) return;
        Container parent = component.getParent();
        if (parent == null || parent instanceof JLayer) return;
        Slot slot = Slot.forChild(parent,component);
        if (slot == null) return;
        JLayer<JComponent> layer = new JLayer<>(null,new GuardUI());
        layers.put(component,new WeakReference<>(layer));
        slot.replace(component,layer);
        layer.setView(component);
        parent.revalidate(); parent.repaint();
    }

    void restore() {
        for (Map.Entry<JComponent,WeakReference<JLayer<JComponent>>> entry : new ArrayList<>(layers.entrySet())) {
            JLayer<JComponent> layer = entry.getValue().get(); JComponent original = entry.getKey();
            if (layer == null || original == null || layer.getParent() == null) continue;
            Slot slot = Slot.forChild(layer.getParent(),layer);
            if (slot == null) continue;
            layer.setView(null); slot.replace(layer,original); slot.parent.revalidate(); slot.parent.repaint();
        }
        layers.clear();
    }

    private static final class GuardUI extends LayerUI<JComponent> {
        @Override public void paint(Graphics graphics,JComponent component) {
            JLayer<?> layer=(JLayer<?>)component;
            if (!(layer.getView() instanceof JComponent)) return;
            JComponent view=(JComponent)layer.getView();
            int width=view.getWidth(),height=view.getHeight(); if (width<=0 || height<=0) return;
            Graphics copy=graphics.create(view.getX(),view.getY(),width,height);
            // These SoapUI toolbar implementations only draw a white fill and divider of their own.
            // Draw that background here; keep their actual child controls and original layout.
            copy.setColor(ThemeEngine.BG);copy.fillRect(0,0,width,height);
            if(view.getBorder()!=null)view.getBorder().paintBorder(view,copy,0,0,width,height);
            for(Component child:view.getComponents()) if(child.isVisible()) {
                Graphics childGraphics=copy.create(child.getX(),child.getY(),child.getWidth(),child.getHeight());
                child.printAll(childGraphics); childGraphics.dispose();
            }
            copy.setColor(ThemeEngine.LINE);
            if(width>height)copy.drawLine(0,height-1,width,height-1);
            copy.dispose();
        }
    }

    /** Only layouts with a public, lossless replacement operation are supported. */
    private static final class Slot {
        final Container parent; final int index; final Object constraint;
        private Slot(Container parent,int index,Object constraint) { this.parent=parent;this.index=index;this.constraint=constraint; }
        static Slot forChild(Container parent,Component child) {
            int index=parent.getComponentZOrder(child); if(index<0)return null;
            if (parent instanceof JSplitPane) return new Slot(parent,index,((JSplitPane)parent).getLeftComponent()==child ? "left":"right");
            if (parent instanceof JViewport) return new Slot(parent,index,null);
            LayoutManager layout=parent.getLayout();
            if(layout instanceof BorderLayout)return new Slot(parent,index,((BorderLayout)layout).getConstraints(child));
            if(layout instanceof GridBagLayout)return new Slot(parent,index,((GridBagLayout)layout).getConstraints(child));
            if(layout instanceof com.jgoodies.forms.layout.FormLayout)
                return new Slot(parent,index,((com.jgoodies.forms.layout.FormLayout)layout).getConstraints(child));
            if(layout instanceof BoxLayout || layout instanceof FlowLayout || layout instanceof GridLayout
                || layout instanceof org.jdesktop.swingx.VerticalLayout || layout instanceof org.jdesktop.swingx.HorizontalLayout || layout==null)
                return new Slot(parent,index,null);
            return null;
        }
        void replace(Component old,Component replacement) {
            Rectangle bounds=old.getBounds();
            if (parent instanceof JSplitPane) {
                if("left".equals(constraint))((JSplitPane)parent).setLeftComponent(replacement);
                else ((JSplitPane)parent).setRightComponent(replacement);
            } else if(parent instanceof JViewport)((JViewport)parent).setView(replacement);
            else { parent.remove(old); parent.add(replacement,constraint,Math.min(index,parent.getComponentCount())); }
            if(parent.getLayout()==null)replacement.setBounds(bounds);
        }
    }
}
