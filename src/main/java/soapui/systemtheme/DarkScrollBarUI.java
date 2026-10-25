// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Avoids cached light gradients in the old Plastic scrollbar delegate. */
final class DarkScrollBarUI extends BasicScrollBarUI {
    @Override protected void configureScrollBarColors() {
        super.configureScrollBarColors();thumbColor=ThemeEngine.LINE;trackColor=ThemeEngine.INPUT;
    }
    @Override protected void paintTrack(Graphics g,JComponent c,Rectangle track) {
        g.setColor(ThemeEngine.INPUT);g.fillRect(track.x,track.y,track.width,track.height);
    }
    @Override protected void paintThumb(Graphics g,JComponent c,Rectangle thumb) {
        if(!scrollbar.isEnabled() || thumb.width<=0 || thumb.height<=0)return;
        g.setColor(isThumbRollover()?ThemeEngine.MUTED:ThemeEngine.LINE);
        g.fillRoundRect(thumb.x+2,thumb.y+2,Math.max(0,thumb.width-4),Math.max(0,thumb.height-4),6,6);
    }
    @Override protected JButton createDecreaseButton(int orientation) { return arrow(orientation); }
    @Override protected JButton createIncreaseButton(int orientation) { return arrow(orientation); }
    private JButton arrow(int orientation) {
        return new BasicArrowButton(orientation,ThemeEngine.BG,ThemeEngine.LINE,ThemeEngine.FG,ThemeEngine.BG);
    }
}
