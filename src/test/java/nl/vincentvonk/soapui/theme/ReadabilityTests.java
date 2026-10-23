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

/** Read-only HTML and the real SoapUI tab-layout icon, without altering exception handling. */
public final class ReadabilityTests {
    private static int checks;
    private static void check(boolean condition,String message) { checks++;if(!condition)throw new AssertionError(message); }
    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(() -> {try {run(Path.of(args[0]));}catch(Exception e){throw new RuntimeException(e);}});
            System.out.println("PASS: "+checks+" error-text, layout-icon, document/action preservation and restoration checks");System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
    private static void run(Path output) throws Exception {
        Files.createDirectories(output);UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        JDialog dialog=new JDialog((Frame)null,"Error",false);
        dialog.setFocusableWindowState(false);dialog.setAutoRequestFocus(false);dialog.setLocation(-10000,-10000);
        dialog.add(new DescriptionPanel("Error","An error of type NullPointerException occurred.",null),BorderLayout.NORTH);
        JEditorPane error=new JEditorPane("text/html","<html><body>java.lang.NullPointerException: Cannot invoke<br>javax.swing.JMenu.removeAll() because curMenu is null</body></html>");
        error.setEditable(false);
        Document document=error.getDocument();String content=document.getText(0,document.getLength());
        Color foreground=error.getForeground();
        dialog.add(new JScrollPane(error),BorderLayout.CENTER);
        Icon original=UISupport.createImageIcon("/toggle_tabs.gif");
        check(original!=null&&original.getIconWidth()>0,"Real SoapUI tab-layout icon is available");
        BufferedImage sourceBefore=icon(original);
        int[] actions={0};Action action=new AbstractAction("",original) {
            public void actionPerformed(java.awt.event.ActionEvent e){actions[0]++;}
        };
        action.putValue(Action.SHORT_DESCRIPTION,"Toggles to tab-based layout");
        JButton tabs=UISupport.createToolbarButton(action);Icon disabled=tabs.getDisabledIcon();ButtonModel model=tabs.getModel();
        JPanel controls=new JPanel(new FlowLayout());controls.add(tabs);dialog.add(controls,BorderLayout.SOUTH);
        JEditorPane editable=new JEditorPane("text/html","<html><body>Unsaved editable HTML</body></html>");
        editable.setEditable(true);Document editableDocument=editable.getDocument();
        ThemeEngine engine=new ThemeEngine();dialog.setSize(480,260);dialog.setVisible(true);
        for(int cycle=0;cycle<3;cycle++) {
            engine.setDark(true,dialog);dialog.validate();
            check(Boolean.TRUE.equals(error.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES)),"Read-only HTML honors the foreground");
            check(error.getForeground().equals(ThemeEngine.FG),"Error text has a light foreground");
            check(error.getDocument()==document&&document.getText(0,document.getLength()).equals(content),"Error document identity and content preserved");
            error.setCaretPosition(3);error.moveCaretPosition(9);engine.patchTree(error);
            check(error.getCaret().getDot()==9&&error.getCaret().getMark()==3,"Reapplying message styling preserves copy selection");
            BufferedImage text=render(error);
            check(brightPixels(text)>40,"Actual HTML text renders as light pixels");
            check(new Color(text.getRGB(text.getWidth()-2,text.getHeight()-2)).getRed()<80,"HTML background remains dark");
            check(tabs.getIcon() instanceof TabsLayoutIcon&&tabs.getDisabledIcon() instanceof TabsLayoutIcon,"Normal and disabled layout glyphs are themed");
            check(tabs.getIcon().getIconWidth()==original.getIconWidth()&&tabs.getIcon().getIconHeight()==original.getIconHeight(),"Original icon dimensions retained");
            check(whitePixels(icon(tabs.getIcon()))==0&&whitePixels(icon(tabs.getDisabledIcon()))==0,"Layout glyph has no white face in either state");
            check(brightPixels(icon(tabs.getIcon()))>10,"Layout glyph remains readable");
            check(tabs.getAction()==action&&tabs.getModel()==model,"Actual action and model retained");
            engine.patchTree(tabs);tabs.doClick();check(actions[0]==cycle+1,"Action invoked exactly once");
            check(samePixels(sourceBefore,icon(original)),"Host icon image was not modified");
            engine.patchTree(editable);
            check(editable.getDocument()==editableDocument&&editable.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES)==null,"Editable HTML document and display property untouched");
            if(cycle==0) {ImageIO.write(render(dialog),"png",output.resolve("error-and-layout-dark.png").toFile());ImageIO.write(icon(tabs.getIcon()),"png",output.resolve("layout-icon-dark.png").toFile());}
            engine.setDark(false,dialog);dialog.validate();
            check(error.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES)==null,"Original unset HTML property restored");
            check(error.getForeground().equals(foreground),"Original HTML foreground restored");
            check(error.getDocument()==document&&document.getText(0,document.getLength()).equals(content),"Error document preserved in Light");
            check(tabs.getIcon()==original&&tabs.getDisabledIcon()==disabled,"Original layout icons restored by identity");
            if(cycle==0)ImageIO.write(render(dialog),"png",output.resolve("error-and-layout-restored.png").toFile());
        }
        engine.setDark(true,dialog);
        JEditorPane late=new JEditorPane("text/html","<html><body>Opened at night</body></html>");late.setEditable(false);
        late.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.FALSE);Document lateDocument=late.getDocument();
        controls.add(late);engine.patchTree(late);
        check(Boolean.TRUE.equals(late.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES)),"New read-only HTML styled at night");
        engine.setDark(false,dialog);
        check(Boolean.FALSE.equals(late.getClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES))&&late.getDocument()==lateDocument,"Explicit false HTML property and late document restored");
        dialog.dispose();
    }
    private static BufferedImage render(Component component) {
        BufferedImage image=new BufferedImage(Math.max(1,component.getWidth()),Math.max(1,component.getHeight()),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();component.printAll(g);g.dispose();return image;
    }
    private static BufferedImage icon(Icon icon) {
        BufferedImage image=new BufferedImage(icon.getIconWidth(),icon.getIconHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();icon.paintIcon(null,g,0,0);g.dispose();return image;
    }
    private static int brightPixels(BufferedImage image) {
        int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            Color c=new Color(image.getRGB(x,y),true);if(c.getAlpha()>0&&c.getRed()>140&&c.getGreen()>140&&c.getBlue()>140)count++;
        }return count;
    }
    private static int whitePixels(BufferedImage image) {
        int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            Color c=new Color(image.getRGB(x,y),true);if(c.getAlpha()>0&&c.getRed()>235&&c.getGreen()>235&&c.getBlue()>235)count++;
        }return count;
    }
    private static boolean samePixels(BufferedImage a,BufferedImage b) {
        for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)if(a.getRGB(x,y)!=b.getRGB(x,y))return false;
        return true;
    }
}
