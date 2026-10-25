// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import com.eviware.soapui.support.xml.SyntaxEditorUtil;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyBluer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.Document;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public final class ThemeTests {
    private static int assertions;
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        try { run(args); System.exit(0); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    private static void run(String[] args) throws Exception {
        check(WindowsAppTheme.parse("\n    AppsUseLightTheme    REG_DWORD    0x0\r\n").equals(Optional.of(true)), "dark registry parsing");
        check(WindowsAppTheme.parse("    AppsUseLightTheme    REG_DWORD    0x1").equals(Optional.of(false)), "light registry parsing");
        check(WindowsAppTheme.parse("AppsUseLightTheme REG_DWORD 0X0").equals(Optional.of(true)), "case insensitive parsing");
        check(WindowsAppTheme.parse("AppsUseLightTheme REG_DWORD 7").isEmpty(), "unsupported value must not force a switch");
        check(WindowsAppTheme.parse("Access denied").isEmpty(), "query errors must not force light");
        Optional<Boolean> live = new WindowsAppTheme().read();
        check(live.isPresent(), "read real Windows app setting");
        System.out.println("Windows app mode read successfully: " + (live.get() ? "dark" : "light"));
        SwingUtilities.invokeAndWait(() -> {
            try { exercise(Path.of(args[0])); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        System.out.println("PASS: " + assertions + " assertions (registry, real SoapUI editors, rendering and repeated light/dark transitions)");
    }

    private static void exercise(Path directory) throws Exception {
        Files.createDirectories(directory);
        PlasticLookAndFeel.setPlasticTheme(new SkyBluer());
        UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        UIManager.put("Panel.background", Color.WHITE); // SoapUI overrides these outside its L&F.
        UIManager.put("TextArea.background", Color.WHITE);
        Object originalPanelDefault = UIManager.get("Panel.background");
        LookAndFeel originalLaf = UIManager.getLookAndFeel();
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(244,244,244));
        JMenuBar bar = new JMenuBar(); JMenu menu = new JMenu("File"); menu.add("Save"); bar.add(menu); root.add(bar, BorderLayout.NORTH);
        RSyntaxTextArea editor = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        editor.setText("<payment currency=\"EUR\">\n  <amount>125.00</amount>\n</payment>");
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.setCaretPosition(8); editor.moveCaretPosition(16);
        Document document = editor.getDocument();
        Color originalBackground = editor.getBackground();
        Color originalToken = editor.getSyntaxScheme().getStyle(org.fife.ui.rsyntaxtextarea.TokenTypes.MARKUP_TAG_NAME).foreground;
        Font font = editor.getFont();
        RTextScrollPane scroll = new RTextScrollPane(editor); root.add(scroll, BorderLayout.CENTER);
        JTable table = new JTable(new Object[][]{{"HTTP", "200 OK"},{"Duration", "23 ms"}}, new String[]{"Property", "Value"});
        TableCellRenderer originalRenderer = table.getDefaultRenderer(Object.class);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(new JScrollPane(table), BorderLayout.CENTER);
        bottom.setPreferredSize(new Dimension(700,110));
        JButton button = new JButton("Send request"); bottom.add(button, BorderLayout.SOUTH); root.add(bottom, BorderLayout.SOUTH);
        ThemeEngine engine = new ThemeEngine();
        JFrame fixture = new JFrame("System Theme automated component test");
        fixture.setFocusableWindowState(false); fixture.setAutoRequestFocus(false);
        JMenuBar paintedWhiteBar = new JMenuBar() {
            @Override protected void paintComponent(Graphics g) { super.paintComponent(g); g.setColor(Color.WHITE); g.fillRect(0,0,getWidth(),getHeight()); }
        };
        JMenu fixtureMenu = new JMenu("Fixture menu"); JMenuItem action = new JMenuItem("Existing action");
        fixtureMenu.add(action); paintedWhiteBar.add(fixtureMenu); fixture.setJMenuBar(paintedWhiteBar);
        fixture.setContentPane(root); fixture.setSize(760,460); fixture.setLocation(-10000,-10000); fixture.setVisible(true);
        render(root, directory.resolve("light-before.png"));
        String text = editor.getText(); int dot = editor.getCaret().getDot(), mark = editor.getCaret().getMark();
        for (int cycle = 0; cycle < 4; cycle++) {
            engine.setDark(true, fixture);
            check(fixture.getJMenuBar() == paintedWhiteBar, "original menu bar retained in dark");
            check(fixture.getJMenuBar().getMenu(0).getItem(0) == action, "existing menu action retained");
            check(engine.isDark(), "engine dark state");
            check(editor.getBackground().equals(ThemeEngine.INPUT), "XML editor dark background");
            check(editor.getCaretColor().equals(ThemeEngine.FG), "visible caret");
            check(root.getBackground().equals(ThemeEngine.BG), "explicit SoapUI-style background overridden");
            check(editor.getDocument() == document && editor.getText().equals(text), "document identity and content preserved in dark");
            check(editor.getCaret().getDot() == dot && editor.getCaret().getMark() == mark, "selection and caret preserved in dark");
            check(editor.getFont().equals(font), "editor font preserved");
            Component renderer = table.prepareRenderer(table.getCellRenderer(0,0), 0,0);
            check(renderer.getBackground().equals(ThemeEngine.BG) || renderer.getBackground().equals(ThemeEngine.INPUT), "table renderer dark");
            JPanel added = new JPanel(); added.add(new JTextField("Opened in dark mode")); bottom.add(added, BorderLayout.NORTH);
            engine.patchTree(added);
            check(added.getBackground().equals(ThemeEngine.BG), "new component themed");
            if (cycle == 0) render(root, directory.resolve("dark.png"));
            engine.setDark(false, fixture);
            check(fixture.getJMenuBar() == paintedWhiteBar, "original menu bar restored");
            check(UIManager.getLookAndFeel() == originalLaf, "original look-and-feel instance restored");
            check(UIManager.get("Panel.background").equals(originalPanelDefault), "developer defaults restored");
            check(editor.getBackground().equals(originalBackground), "original editor background restored");
            check(editor.getSyntaxScheme().getStyle(org.fife.ui.rsyntaxtextarea.TokenTypes.MARKUP_TAG_NAME).foreground.equals(originalToken), "syntax token colours restored");
            check(editor.getDocument() == document && editor.getText().equals(text), "document preserved in light");
            check(editor.getCaret().getDot() == dot && editor.getCaret().getMark() == mark, "selection and caret preserved in light");
            check(table.getDefaultRenderer(Object.class) == originalRenderer, "original renderer restored");
            Component lightCell = table.prepareRenderer(table.getCellRenderer(0,0),0,0);
            check(!lightCell.getBackground().equals(ThemeEngine.INPUT) && !lightCell.getBackground().equals(ThemeEngine.BG), "table cell returns to light mode");
            check(!added.getBackground().equals(ThemeEngine.BG), "new-at-night component restored to daytime default");
            bottom.remove(added);
        }
        render(root, directory.resolve("light-restored.png"));
        fixture.dispose();
    }

    private static void render(JComponent root, Path path) throws Exception {
        root.setSize(760, 460); layout(root);
        BufferedImage image = new BufferedImage(760,460,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); root.printAll(g); g.dispose();
        ImageIO.write(image,"png",path.toFile());
    }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container) layout((Container)child); }
}
