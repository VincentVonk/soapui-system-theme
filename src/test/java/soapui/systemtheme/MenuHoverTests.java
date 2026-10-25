// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Verify incremental onscreen painting, which a full printAll snapshot cannot exercise. */
public final class MenuHoverTests {
    private static JFrame frame;
    private static JMenuBar bar;
    private static JLabel marker;
    private static ThemeEngine engine;
    private static JMenu[] menus;
    private static int checks, actions;
    private static final Color MARKER = new Color(0x1fc9b3);
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        Thread watchdog = new Thread(() -> {
            try { Thread.sleep(30000); } catch (InterruptedException e) { return; }
            System.err.println("Menu hover test timed out"); System.exit(2);
        }, "menu-hover-test-watchdog");
        watchdog.setDaemon(true); watchdog.start();
        try { run(Path.of(args[0])); System.exit(0); }
        catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
    private static void run(Path output) throws Exception {
        Files.createDirectories(output);
        LayoutManager[] originalLayout = new LayoutManager[1];
        JMenuItem[] originalAction = new JMenuItem[1];
        SwingUtilities.invokeAndWait(() -> {
            try { UIManager.setLookAndFeel(new com.jgoodies.looks.plastic.PlasticXPLookAndFeel()); }
            catch (Exception e) { throw new RuntimeException(e); }
            frame = new JFrame("Isolated menu hover regression");
            frame.setFocusableWindowState(false); frame.setAutoRequestFocus(false);
            frame.setAlwaysOnTop(true); frame.setBounds(50,50,700,200);
            bar = new JMenuBar() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g); g.setColor(Color.WHITE); g.fillRect(0,0,getWidth(),getHeight());
                }
            };
            menus = new JMenu[]{new JMenu("File"),new JMenu("Project"),new JMenu("Suite"),new JMenu("Case"),new JMenu("Theme")};
            for (JMenu menu : menus) { bar.add(menu); menu.add("Synthetic action"); }
            menus[2].setEnabled(false); menus[3].setEnabled(false);
            originalAction[0] = menus[0].getItem(0);
            originalAction[0].setAccelerator(KeyStroke.getKeyStroke("ctrl Y"));
            originalAction[0].addActionListener(e -> actions++);
            frame.setJMenuBar(bar);
            marker = new JLabel("Synthetic content only"); marker.setOpaque(true); marker.setBackground(MARKER);
            marker.setPreferredSize(new Dimension(100,16)); frame.add(marker,BorderLayout.SOUTH);
            frame.setVisible(true);
            originalLayout[0] = frame.getRootPane().getLayout();
            engine = new ThemeEngine();
        });
        try {
            for (int cycle=0; cycle<3; cycle++) {
                SwingUtilities.invokeAndWait(() -> {
                    try { engine.setDark(true,frame); } catch (Exception e) { throw new RuntimeException(e); }
                    marker.setBackground(MARKER);
                    frame.validate(); frame.getRootPane().paintImmediately(0,0,frame.getWidth(),frame.getHeight());
                });
                capture(false, "initial dark", output.resolve("menu-dark-initial.png"));
                for (int i=0; i<menus.length; i++) {
                    JMenu menu = menus[i];
                    SwingUtilities.invokeAndWait(() -> {
                        menu.dispatchEvent(new MouseEvent(menu,MouseEvent.MOUSE_ENTERED,System.currentTimeMillis(),0,4,4,0,false));
                        menu.repaint();
                        RepaintManager.currentManager(menu).paintDirtyRegions();
                        // Also force the parent's dirty region: it has the host's hardcoded white paint.
                        bar.paintImmediately(0,0,bar.getWidth(),bar.getHeight());
                    });
                    capture(false, "hover/partial repaint " + menu.getText(), output.resolve("menu-hover-dark.png"));
                    SwingUtilities.invokeAndWait(() -> menu.dispatchEvent(new MouseEvent(menu,MouseEvent.MOUSE_EXITED,System.currentTimeMillis(),0,4,4,0,false)));
                }
                SwingUtilities.invokeAndWait(() -> {
                    menus[0].doClick();
                    check(MenuSelectionManager.defaultManager().getSelectedPath().length>0,"Original menu no longer opens");
                    bar.paintImmediately(0,0,bar.getWidth(),bar.getHeight());
                });
                capture(false,"selected File menu",output.resolve("menu-selected-dark.png"));
                SwingUtilities.invokeAndWait(() -> {
                    MenuSelectionManager.defaultManager().clearSelectedPath();
                    originalAction[0].doClick();
                    frame.setSize(720+actions*5,200); frame.validate();
                    bar.paintImmediately(0,0,bar.getWidth(),bar.getHeight());
                    check(frame.getJMenuBar()==bar,"Menu bar identity changed");
                    check(menus[0].getItem(0)==originalAction[0],"Action identity changed");
                    check(KeyStroke.getKeyStroke("ctrl Y").equals(originalAction[0].getAccelerator()),"Accelerator changed");
                });
                capture(false,"resized menu",output.resolve("menu-resized-dark.png"));
                SwingUtilities.invokeAndWait(() -> {
                    try { engine.setDark(false,frame); } catch (Exception e) { throw new RuntimeException(e); }
                    marker.setBackground(MARKER); frame.validate();
                    check(frame.getRootPane().getLayout()==originalLayout[0],"Root layout not restored");
                    check(bar.getParent()==frame.getLayeredPane(),"Original menu parent not restored");
                    bar.paintImmediately(0,0,bar.getWidth(),bar.getHeight());
                });
                capture(true,"restored light menu",output.resolve("menu-restored-light.png"));
            }
            check(actions==3,"Original action did not execute after each cycle");
        } finally {
            SwingUtilities.invokeAndWait(() -> { MenuSelectionManager.defaultManager().clearSelectedPath(); frame.dispose(); });
        }
        System.out.println("PASS: " + checks + " onscreen menu hover, partial repaint, selection, resize and restoration checks");
    }
    private static void capture(boolean light, String description, Path file) throws Exception {
        Robot robot = new Robot();
        Rectangle[] bounds = new Rectangle[1]; Point[] tag = new Point[1]; Rectangle[] menu = new Rectangle[1];
        SwingUtilities.invokeAndWait(() -> {
            bounds[0]=frame.getBounds(); tag[0]=SwingUtilities.convertPoint(marker,marker.getWidth()/2,marker.getHeight()/2,frame);
            menu[0]=SwingUtilities.convertRectangle(bar.getParent(),bar.getBounds(),frame);
        });
        long deadline=System.nanoTime()+3_000_000_000L;
        BufferedImage image;
        do {
            robot.delay(120); image=robot.createScreenCapture(bounds[0]);
            if (new Color(image.getRGB(tag[0].x,tag[0].y)).equals(MARKER)) break;
            if (System.nanoTime()>deadline) throw new AssertionError("Test frame is obscured: " + description);
        } while (true);
        ImageIO.write(image,"png",file.toFile());
        int bright=0,total=0;
        Rectangle r=menu[0];
        for (int y=r.y+2;y<r.y+r.height-2;y++) for (int x=r.x+2;x<r.x+r.width-2;x++) {
            Color c=new Color(image.getRGB(x,y));
            if (c.getRed()>210&&c.getGreen()>210&&c.getBlue()>210) bright++;
            total++;
        }
        double ratio=(double)bright/total;
        check(light ? ratio>.65 : ratio<.15, description + " has wrong menu background: bright ratio=" + ratio);
    }
}
