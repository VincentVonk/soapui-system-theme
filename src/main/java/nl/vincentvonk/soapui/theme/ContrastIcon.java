// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.Icon;

/** Brighten only dark neutral icon pixels; colour status signals and the source image are preserved. */
final class ContrastIcon implements Icon {
    private final Icon source;
    private final boolean disabled;
    ContrastIcon(Icon source,boolean disabled) { this.source=source;this.disabled=disabled; }
    static Icon wrap(Icon source,boolean disabled) { return source==null || source instanceof ContrastIcon || source instanceof TitleControlIcon ? source : new ContrastIcon(source,disabled); }
    public int getIconWidth() { return source.getIconWidth(); }
    public int getIconHeight() { return source.getIconHeight(); }
    public void paintIcon(Component component,Graphics graphics,int x,int y) {
        int width=getIconWidth(),height=getIconHeight(); if(width<=0 || height<=0)return;
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();source.paintIcon(component,g,0,0);g.dispose();
        int[] pixels=((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        int target=disabled ? 132 : 208;
        for(int i=0;i<pixels.length;i++) {
            int p=pixels[i],r=(p>>16)&255,green=(p>>8)&255,b=p&255;
            int max=Math.max(r,Math.max(green,b)),min=Math.min(r,Math.min(green,b));
            if(max-min<38 && max<150)pixels[i]=(p&0xff000000)|(target<<16)|(target<<8)|target;
        }
        graphics.drawImage(image,x,y,null);
    }
}
