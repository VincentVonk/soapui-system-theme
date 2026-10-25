// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import javax.swing.Icon;

/** Original dimensions and button action; a dark face replaces the legacy white icon rim. */
final class HelpGlyphIcon implements Icon {
    private final Icon original;
    private final boolean disabled;
    HelpGlyphIcon(Icon original, boolean disabled) { this.original=original; this.disabled=disabled; }
    public int getIconWidth() { return original.getIconWidth(); }
    public int getIconHeight() { return original.getIconHeight(); }
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.translate(x,y); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int w=getIconWidth(),h=getIconHeight(),d=Math.max(1,Math.min(w,h)-3);
        g.setColor(ThemeEngine.BG); g.fillOval((w-d)/2,(h-d)/2,d,d);
        g.setColor(disabled?ThemeEngine.MUTED:new Color(0x8bbcff)); g.drawOval((w-d)/2,(h-d)/2,d,d);
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,Math.max(9,d-2)));
        FontMetrics fm=g.getFontMetrics();
        g.drawString("?",(w-fm.stringWidth("?"))/2,(h-fm.getHeight())/2+fm.getAscent());
        g.dispose();
    }
}
