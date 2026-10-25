// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/** Real internal-window state actions and icon rendering, including controls created at night. */
public final class TitleControlTests {
    private static int checks;
    private static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    public static void main(String[] args) {
        try { SwingUtilities.invokeAndWait(()->{try{run();}catch(Exception e){throw new RuntimeException(e);}});
            System.out.println("PASS: "+checks+" internal-window control rendering, state actions and restoration checks");System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
    private static JButton button(JInternalFrame frame,String role) {
        JComponent title=((BasicInternalFrameUI)frame.getUI()).getNorthPane();
        for(Component c:title.getComponents())if(c instanceof JButton)for(java.awt.event.ActionListener l:((JButton)c).getActionListeners())
            if(l.getClass().getName().endsWith("$"+role+"Action"))return (JButton)c;
        throw new AssertionError("Button missing: "+role);
    }
    private static void darkButtons(JInternalFrame frame) {
        for(String role:new String[]{"Close","Maximize","Iconify"}) {
            JButton button=button(frame,role);Icon icon=button.getIcon();
            check(icon instanceof TitleControlIcon,"flat "+role+" icon installed");
            check(ContrastIcon.wrap(icon,false)==icon,"dark background is not brightened again");
            BufferedImage image=new BufferedImage(icon.getIconWidth(),icon.getIconHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();icon.paintIcon(button,g,0,0);g.dispose();
            Color pixel=new Color(image.getRGB(1,1));
            check(pixel.getRed()<80&&pixel.getGreen()<80&&pixel.getBlue()<80,"dark control face: "+role);
        }
    }
    private static void run() throws Exception {
        UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        Map<String,Object> oldDefaults=new LinkedHashMap<>();
        for(String key:new String[]{"closeIcon","maximizeIcon","minimizeIcon","iconifyIcon"})
            oldDefaults.put("InternalFrame."+key,UIManager.get("InternalFrame."+key));
        JFrame host=new JFrame("Internal controls");host.setFocusableWindowState(false);host.setAutoRequestFocus(false);host.setLocation(-10000,-10000);
        JDesktopPane desktop=new JDesktopPane();host.add(desktop);host.setSize(700,480);host.setVisible(true);
        JInternalFrame frame=new JInternalFrame("Request",true,true,true,true);frame.setSize(500,300);desktop.add(frame);frame.setVisible(true);
        ThemeEngine theme=new ThemeEngine();
        for(int cycle=0;cycle<3;cycle++) {
            theme.setDark(true,host);darkButtons(frame);
            JButton maximize=button(frame,"Maximize"),minimize=button(frame,"Iconify"),close=button(frame,"Close");
            java.awt.event.ActionListener[] original=maximize.getActionListeners();
            maximize.doClick();check(frame.isMaximum(),"maximize action works");darkButtons(frame);
            check(button(frame,"Maximize")==maximize&&Arrays.equals(original,maximize.getActionListeners()),"same maximize button and listeners retained");
            maximize.doClick();check(!frame.isMaximum(),"restore action works");darkButtons(frame);
            minimize.doClick();check(frame.isIcon(),"minimize action works");frame.setIcon(false);darkButtons(frame);
            check(button(frame,"Close")==close,"close button retained across state changes");
            JInternalFrame late=new JInternalFrame("New at night",true,true,true,true);late.setBounds(10,10,300,200);desktop.add(late);late.setVisible(true);theme.patchTree(late);darkButtons(late);
            button(late,"Close").doClick();check(late.isClosed(),"original close action works");
            theme.setDark(false,host);
            for(Map.Entry<String,Object> entry:oldDefaults.entrySet())check(UIManager.get(entry.getKey())==entry.getValue(),"original icon default restored: "+entry.getKey());
            check(!(button(frame,"Close").getIcon() instanceof TitleControlIcon),"live window returns to its original icon style");
        }
        host.dispose();
    }
}
