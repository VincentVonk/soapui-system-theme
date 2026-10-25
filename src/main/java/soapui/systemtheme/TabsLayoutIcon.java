// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import javax.swing.Icon;

/** Dark, original two-panel glyph; retains the host icon's size and button action. */
final class TabsLayoutIcon implements Icon {
    private final Icon original;
    private final boolean disabled;
    TabsLayoutIcon(Icon original, boolean disabled) { this.original=original;this.disabled=disabled; }
    public int getIconWidth() { return original.getIconWidth(); }
    public int getIconHeight() { return original.getIconHeight(); }
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
        int w=getIconWidth(),h=getIconHeight();
        g.setColor(ThemeEngine.BG);g.fillRect(0,0,w,h);
        Color line=disabled?ThemeEngine.MUTED:ThemeEngine.FG;
        g.setColor(line);g.drawRect(2,5,Math.max(1,w-7),Math.max(1,h-8));
        g.setColor(ThemeEngine.BG);g.fillRect(5,2,Math.max(1,w-7),Math.max(1,h-6));
        g.setColor(line);g.drawRect(5,2,Math.max(1,w-7),Math.max(1,h-6));
        g.drawLine(6,5,Math.max(6,w-3),5);g.dispose();
    }
}
