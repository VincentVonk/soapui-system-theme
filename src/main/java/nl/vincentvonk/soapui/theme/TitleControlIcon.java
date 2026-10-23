// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.UIResource;

/** Flat dark internal-window controls. The original Swing buttons and their actions stay in place. */
final class TitleControlIcon implements Icon, UIResource {
    enum Kind { CLOSE, MAXIMIZE, RESTORE, MINIMIZE }
    private final Kind kind;
    TitleControlIcon(Kind kind) { this.kind=kind; }
    public int getIconWidth() { return 16; }
    public int getIconHeight() { return 16; }
    public void paintIcon(Component component,Graphics graphics,int x,int y) {
        Graphics2D g=(Graphics2D)graphics.create(x,y,16,16);
        ButtonModel model=component instanceof AbstractButton ? ((AbstractButton)component).getModel() : null;
        boolean pressed=model!=null && model.isPressed() && model.isArmed();
        g.setColor(pressed ? ThemeEngine.SELECT : ThemeEngine.BG);g.fillRect(0,0,16,16);
        g.setColor(ThemeEngine.LINE);g.drawRect(0,0,15,15);
        g.setColor(component!=null && !component.isEnabled() ? ThemeEngine.MUTED : ThemeEngine.FG);
        switch(kind) {
            case CLOSE -> { g.drawLine(4,4,11,11);g.drawLine(4,11,11,4); }
            case MAXIMIZE -> { g.drawRect(3,3,9,9);g.drawLine(3,5,12,5); }
            case RESTORE -> {
                g.drawRect(6,3,6,6);g.setColor(pressed?ThemeEngine.SELECT:ThemeEngine.BG);g.fillRect(3,6,7,7);
                g.setColor(component!=null&&!component.isEnabled()?ThemeEngine.MUTED:ThemeEngine.FG);
                g.drawRect(3,6,6,6);g.drawLine(3,8,9,8);
            }
            case MINIMIZE -> { g.fillRect(4,11,8,2); }
        }
        g.dispose();
    }
}
