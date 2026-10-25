// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import com.eviware.soapui.impl.RoundButton;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.swing.JTableFactory;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;

/** Regression coverage for the actual fixed-white SoapUI toolbar and post-render table stripes. */
public final class LegacyPaintTests {
    private static int checks;
    private static void check(boolean value,String name) { checks++;if(!value)throw new AssertionError(name); }
    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(() -> { try{test();}catch(Exception e){throw new RuntimeException(e);} });
            System.out.println("PASS: "+checks+" legacy paint, actions, icon and renderer restoration checks");System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
    private static void test() throws Exception {
        UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        JFrame frame=new JFrame("Legacy paint tests");frame.setFocusableWindowState(false);frame.setAutoRequestFocus(false);
        JPanel root=new JPanel(new BorderLayout());frame.setContentPane(root);
        JXToolBar toolbar=new JXToolBar();toolbar.setFloatable(false);toolbar.setPreferredSize(new Dimension(700,50));
        BufferedImage source=new BufferedImage(12,12,BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0,0,0xff333333);source.setRGB(1,0,0xffff0000);
        ImageIcon originalIcon=new ImageIcon(source);
        final int[] actions={0};
        JButton action=new JButton(new AbstractAction("Existing action",originalIcon) {
            public void actionPerformed(java.awt.event.ActionEvent e){actions[0]++;}
        });toolbar.add(action);
        JPanel coloredPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        RoundButton blue=new RoundButton(6);blue.setText("Endpoint Explorer");blue.setBackground(new Color(52,137,209));
        blue.setForeground(Color.WHITE);coloredPanel.add(blue);toolbar.add(coloredPanel);root.add(toolbar,BorderLayout.NORTH);
        JTable table=new JTable(new Object[][]{{"HTTP","200"},{"Duration","23ms"}},new String[]{"Name","Value"}) {
            @Override public Component prepareRenderer(TableCellRenderer renderer,int row,int col) {
                Component cell=super.prepareRenderer(renderer,row,col);
                JTableFactory.applyStripesToRenderer(row,cell);return cell;
            }
        };
        JTree tree=new JTree();DefaultTreeCellRenderer originalTree=(DefaultTreeCellRenderer)tree.getCellRenderer();
        Color originalTreeColor=originalTree.getTextNonSelectionColor();
        root.add(new JScrollPane(table),BorderLayout.CENTER);root.add(new JScrollPane(tree),BorderLayout.WEST);
        frame.setSize(700,400);frame.setLocation(-10000,-10000);frame.setVisible(true);
        ThemeEngine engine=new ThemeEngine();
        javax.swing.plaf.ScrollBarUI originalScroll=((JScrollPane)tree.getParent().getParent()).getVerticalScrollBar().getUI();
        for(int cycle=0;cycle<3;cycle++) {
            engine.setDark(true,frame);frame.validate();
            check(toolbar.getParent() instanceof JLayer,"real toolbar kept inside a paint layer");
            check(action.getParent()==toolbar,"original action component retained");
            Point hit=SwingUtilities.convertPoint(action,action.getWidth()/2,action.getHeight()/2,root);
            check(SwingUtilities.getDeepestComponentAt(root,hit.x,hit.y)==action,"mouse target still reaches original button");
            action.doClick();check(actions[0]==cycle+1,"original action still runs exactly once");
            Component cell=table.prepareRenderer(table.getCellRenderer(0,0),0,0);
            check(cell.getBackground().equals(ThemeEngine.INPUT),"post-render white stripes cannot undo dark colours");
            table.setRowSelectionInterval(0,0);cell=table.prepareRenderer(table.getCellRenderer(0,0),0,0);
            check(cell.getBackground().equals(ThemeEngine.SELECT),"selection survives post-render white stripes");table.clearSelection();
            BufferedImage output=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=output.createGraphics();root.printAll(g);g.dispose();
            Point p=SwingUtilities.convertPoint(blue,4,4,root);Color pixel=new Color(output.getRGB(p.x,p.y));
            check(pixel.getBlue()>140 && pixel.getBlue()>pixel.getRed()+80,"coloured child control remains visible");
            BufferedImage icon=new BufferedImage(12,12,BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig=icon.createGraphics();action.getIcon().paintIcon(action,ig,0,0);ig.dispose();
            check((icon.getRGB(0,0)&255)>150,"dark neutral icon pixels brightened");
            check(icon.getRGB(1,0)==0xffff0000,"semantic red icon pixels unchanged");
            check(source.getRGB(0,0)==0xff333333,"source icon image unchanged");
            engine.setDark(false,frame);frame.validate();
            check(toolbar.getParent()==root,"toolbar restored to original parent");
            check(action.getIcon()==originalIcon,"original icon object restored");
            check(tree.getCellRenderer()==originalTree,"original tree renderer restored");
            check(originalTree.getTextNonSelectionColor().equals(originalTreeColor),"original tree colours restored");
            check(((JScrollPane)tree.getParent().getParent()).getVerticalScrollBar().getUI()==originalScroll,"original scroll delegate restored");
        }
        frame.dispose();
    }
}
