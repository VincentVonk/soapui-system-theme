// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import com.eviware.soapui.support.DescriptionPanel;
import com.eviware.soapui.support.UISupport;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.Document;

public final class DialogThemeTests {
    private static int checks;
    private static void check(boolean value,String message) { checks++; if(!value)throw new AssertionError(message); }
    public static void main(String[] args) {
        try { SwingUtilities.invokeAndWait(() -> {try {run(Path.of(args[0]));}catch(Exception e){throw new RuntimeException(e);}});
            System.out.println("PASS: "+checks+" dialog header, About, help icon and restoration checks");System.exit(0);
        } catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
    private static void run(Path output) throws Exception {
        Files.createDirectories(output); UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        JDialog dialog=new JDialog((Frame)null,"About SoapUI",false);
        dialog.setFocusableWindowState(false);dialog.setAutoRequestFocus(false);dialog.setLocation(-10000,-10000);
        DescriptionPanel header=new DescriptionPanel("SoapUI Preferences","Set global SoapUI settings",null);
        Color headerBackground=header.getBackground(),headerForeground=header.getForeground();
        JEditorPane about=new JEditorPane("text/html","<html><body style='margin:0;padding:0'><div style='background-image:url(file:/nonexistent.png);width:457px;height:301px'>"
            +"<p style='margin-top:85px;margin-left:30px;color:black'>SoapUI 5.10.0<br>Copyright (C) 2004-2026 smartbear.com<br>"
            +"<a href='http://www.soapui.org'>http://www.soapui.org</a><br>Build Date: 2026-06-02</p></div></body></html>");
        about.setEditable(false);Document original=about.getDocument();String originalHtml=about.getText();
        JButton help=new JButton(UISupport.HELP_ICON);Icon originalIcon=help.getIcon();
        int[] actions={0};help.addActionListener(e->actions[0]++);
        dialog.add(header,BorderLayout.NORTH);dialog.add(about,BorderLayout.CENTER);dialog.add(help,BorderLayout.SOUTH);
        dialog.setSize(600,400);dialog.setVisible(true);
        ThemeEngine engine=new ThemeEngine();
        for(int i=0;i<3;i++) {
            engine.setDark(true,dialog);dialog.validate();
            check(header.isOpaque(),"gradient is bypassed");
            BufferedImage image=render(dialog);Point p=SwingUtilities.convertPoint(header,1,1,dialog);
            Color pixel=new Color(image.getRGB(p.x,p.y));
            check(pixel.getRed()<80 && pixel.getGreen()<80 && pixel.getBlue()<80,"real description header paints dark");
            check(about.getDocument()!=original,"About receives an isolated display document");
            check(about.getText().contains("2004-2026 smartbear.com")&&about.getText().contains("2026-06-02"),"copyright and build date retained");
            check(about.getText().contains("http://www.soapui.org"),"About hyperlink retained");
            check(!about.getText().contains("background-image"),"white splash background removed");
            check(help.getIcon() instanceof HelpGlyphIcon,"help icon has a dark face");help.doClick();
            check(actions[0]==i+1,"help action retained exactly once");
            if(i==0)ImageIO.write(image,"png",output.resolve("dialogs-dark.png").toFile());
            engine.setDark(false,dialog);dialog.validate();
            check(!header.isOpaque()&&header.getBackground().equals(headerBackground)&&header.getForeground().equals(headerForeground),"original gradient properties restored");
            check(about.getDocument()==original&&about.getText().equals(originalHtml),"exact original About document restored");
            check(help.getIcon()==originalIcon,"original help icon restored");
        }
        // A dialog created at night must also restore its intentional gradient paint colours.
        engine.setDark(true,dialog);
        DescriptionPanel late=new DescriptionPanel("New SOAP Project","Create a SOAP project",null);
        Color lateForeground=late.getForeground();dialog.add(late,BorderLayout.NORTH);engine.patchTree(late);
        engine.setDark(false,dialog);
        check(!late.isOpaque()&&late.getForeground().equals(lateForeground),"night-created gradient restores original paint foreground");
        JEditorPane request=new JEditorPane("text/html",originalHtml);request.setEditable(true);Document requestDoc=request.getDocument();
        dialog.add(request,BorderLayout.CENTER);engine.setDark(true,dialog);
        check(request.getDocument()==requestDoc,"editable request HTML is never rebuilt");engine.setDark(false,dialog);
        dialog.dispose();
    }
    private static BufferedImage render(Component c) {
        BufferedImage image=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();c.printAll(g);g.dispose();return image;
    }
}
