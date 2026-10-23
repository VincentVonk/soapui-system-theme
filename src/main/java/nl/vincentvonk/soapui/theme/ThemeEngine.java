// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.Gutter;

/** All operations run on Swing's EDT. No documents or SoapUI project models are changed. */
final class ThemeEngine {
    static final Color BG = new Color(0x25282e), INPUT = new Color(0x1d2025);
    static final Color FG = new Color(0xe3e6eb), MUTED = new Color(0xa3acb9);
    static final Color LINE = new Color(0x495361), SELECT = new Color(0x345a85);
    private final Map<Component, Snapshot> originals = new WeakHashMap<>();
    private final Map<JFrame, JMenuBar> menuBars = new WeakHashMap<>();
    private final Map<Object, Object> defaultValues = new LinkedHashMap<>();
    private final Map<String,Object> titleIconDefaults = new LinkedHashMap<>();
    private final ToolbarPaintGuard toolbarPaint = new ToolbarPaintGuard();
    private final AboutPaneTheme aboutPanes = new AboutPaneTheme();
    private FxWebTheme webTheme;
    private LookAndFeel originalLaf;
    private PlasticTheme originalPlastic;
    private MetalTheme originalMetal;
    private boolean dark;
    private boolean applying;

    boolean isDark() { return dark; }
    boolean isApplying() { return applying; }
    void close() { if (webTheme != null) webTheme.setDark(false); }
    private static void edt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Theme changes require the EDT");
    }

    void setDark(boolean value, Component... roots) throws UnsupportedLookAndFeelException {
        edt();
        if (dark == value) return;
        applying = true;
        try {
            if (value) {
                originalLaf = UIManager.getLookAndFeel();
                originalPlastic = PlasticLookAndFeel.getPlasticTheme();
                originalMetal = MetalLookAndFeel.getCurrentTheme();
                // Snapshot before updateUI can replace original explicit colours.
                for (Component root : roots) captureTree(root);
                for (Object key : UIManager.getDefaults().keySet().toArray()) {
                    Object entry = UIManager.get(key);
                    if (entry instanceof Color) defaultValues.put(key, entry);
                }
                for (String key : new String[]{"InternalFrame.closeIcon", "InternalFrame.maximizeIcon",
                    "InternalFrame.minimizeIcon", "InternalFrame.iconifyIcon"}) titleIconDefaults.put(key,UIManager.get(key));
                dark = true; // also permits rollback if installing the L&F fails.
                PlasticLookAndFeel.setPlasticTheme(new DarkPlasticTheme());
                UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
                installDefaults();
                for (Component root : roots) {
                    if (root == null) continue;
                    SwingUtilities.updateComponentTreeUI(root);
                    patchTree(root);
                }
            } else {
                dark = false;
                if (webTheme != null) webTheme.setDark(false);
                aboutPanes.restore();
                toolbarPaint.restore();
                restoreMenuBars();
                PlasticLookAndFeel.setPlasticTheme(originalPlastic);
                MetalLookAndFeel.setCurrentTheme(originalMetal);
                if (originalLaf != null) UIManager.setLookAndFeel(originalLaf);
                titleIconDefaults.forEach(UIManager::put);titleIconDefaults.clear();
                defaultValues.forEach(UIManager::put);
                defaultValues.clear();
                for (Component root : roots) if (root != null) SwingUtilities.updateComponentTreeUI(root);
                for (Map.Entry<Component, Snapshot> e : new ArrayList<>(originals.entrySet()))
                    if (e.getKey() != null) e.getValue().restore(e.getKey());
                // Renderer colours are cached by DefaultTableCellRenderer. Restore these last,
                // after component snapshots, so a nightly renderer cannot leave a dark cache behind.
                for (Map.Entry<Component, Snapshot> e : new ArrayList<>(originals.entrySet()))
                    if (e.getKey() instanceof JTable && e.getValue().table != null)
                        e.getValue().table.restore((JTable) e.getKey());
                originals.clear();
                for (Component root : roots) if (root != null) { root.invalidate(); root.validate(); root.repaint(); }
            }
        } finally { applying = false; }
    }

    private void installDefaults() {
        // SoapUI places colours in UIManager's developer overrides. Merely changing L&F leaves them white.
        defaultValues.forEach((key, old) -> {
            String name = key.toString().toLowerCase(java.util.Locale.ROOT);
            Color color = (Color) old;
            Color replacement;
            if (name.contains("selectionbackground") || name.contains("selectedbackground")) replacement = SELECT;
            else if (name.contains("selectionforeground") || name.contains("selectedforeground")) replacement = FG;
            else if (name.contains("disabled") || name.contains("inactiveforeground")) replacement = MUTED;
            else if (name.contains("foreground") || name.endsWith("text") || name.contains("textcolor")) replacement = readable(color);
            else if (name.contains("background") || name.endsWith("control") || name.endsWith("window"))
                replacement = name.contains("text") || name.startsWith("table.") || name.startsWith("tree.") ? INPUT : BG;
            else replacement = neutral(color) ? LINE : readable(color);
            UIManager.put(key, new ColorUIResource(replacement));
        });
        String[] text = {"TextArea", "TextField", "FormattedTextField", "PasswordField", "EditorPane", "TextPane", "Table", "Tree", "List"};
        for (String prefix : text) {
            put(prefix + ".background", INPUT); put(prefix + ".foreground", FG);
            put(prefix + ".selectionBackground", SELECT); put(prefix + ".selectionForeground", FG);
            put(prefix + ".caretForeground", FG);
        }
        put("TabbedPane.selected", BG); put("TabbedPane.unselectedBackground", INPUT);
        put("TabbedPane.foreground", FG); put("Table.gridColor", LINE);
        put("ToolTip.background", BG); put("ToolTip.foreground", FG);
        put("Menu.selectionBackground", SELECT); put("Menu.selectionForeground", FG);
        put("MenuItem.selectionBackground", SELECT); put("MenuItem.selectionForeground", FG);
        UIManager.put("InternalFrame.closeIcon",new TitleControlIcon(TitleControlIcon.Kind.CLOSE));
        UIManager.put("InternalFrame.maximizeIcon",new TitleControlIcon(TitleControlIcon.Kind.MAXIMIZE));
        UIManager.put("InternalFrame.minimizeIcon",new TitleControlIcon(TitleControlIcon.Kind.RESTORE));
        UIManager.put("InternalFrame.iconifyIcon",new TitleControlIcon(TitleControlIcon.Kind.MINIMIZE));
    }

    private void put(String key, Color color) {
        // Existing colour defaults were captured before installing the dark L&F.
        if (!defaultValues.containsKey(key)) defaultValues.put(key, null);
        UIManager.put(key, new ColorUIResource(color));
    }

    private void captureTree(Component c) {
        if (c == null) return;
        originals.computeIfAbsent(c, item -> new Snapshot(item, false));
        if (c instanceof JMenu) captureTree(((JMenu) c).getPopupMenu());
        if (c instanceof Container) for (Component child : ((Container) c).getComponents()) captureTree(child);
    }

    void patchTree(Component c) {
        edt();
        if (!dark || c == null) return;
        if (c instanceof JFrame) replaceMenuBar((JFrame) c);
        Snapshot snapshot = originals.get(c);
        if (snapshot == null) {
            snapshot = new Snapshot(c, true); originals.put(c, snapshot);
        }
        adaptColors(c);
        // GradientPanel uses foreground as a paint colour, not as its text colour.
        // An opaque panel bypasses that gradient; child labels retain readable text.
        if (c instanceof com.eviware.soapui.support.swing.GradientPanel)
            ((JComponent)c).setOpaque(true);
        if (c instanceof JEditorPane) aboutPanes.install((JEditorPane)c);
        // Keep JavaFX optional: headless/disabled-browser SoapUI sessions need no FX toolkit.
        if (c.getClass().getName().equals("javafx.embed.swing.JFXPanel")) {
            if (webTheme == null) webTheme = new FxWebTheme();
            webTheme.setDark(true); webTheme.watch(c);
        }
        if(c instanceof JScrollBar && !(((JScrollBar)c).getUI() instanceof DarkScrollBarUI))
            ((JScrollBar)c).setUI(new DarkScrollBarUI());
        if(c instanceof AbstractButton) {
            AbstractButton button=(AbstractButton)c;
            button.setIcon(snapshot.icon == com.eviware.soapui.support.UISupport.HELP_ICON
                ? new HelpGlyphIcon(snapshot.icon, false) : ContrastIcon.wrap(button.getIcon(),false));
            button.setDisabledIcon(ContrastIcon.wrap(button.getDisabledIcon(),true));
        } else if(c instanceof JLabel) {
            JLabel label=(JLabel)c;
            label.setIcon(ContrastIcon.wrap(label.getIcon(),false));
            label.setDisabledIcon(ContrastIcon.wrap(label.getDisabledIcon(),true));
        }
        if (c instanceof JToolBar && c.getClass().getName().startsWith("com.eviware.soapui."))
            toolbarPaint.install((JComponent)c);
        if (c instanceof JComponent) {
            Border border = ((JComponent) c).getBorder();
            if (border instanceof MatteBorder && ((MatteBorder) border).getMatteColor() != null
                && neutral(((MatteBorder) border).getMatteColor())) {
                java.awt.Insets in = border.getBorderInsets(c);
                ((JComponent) c).setBorder(BorderFactory.createMatteBorder(in.top, in.left, in.bottom, in.right, LINE));
            } else if (border instanceof javax.swing.border.LineBorder
                && neutral(((javax.swing.border.LineBorder)border).getLineColor())) {
                javax.swing.border.LineBorder line=(javax.swing.border.LineBorder)border;
                ((JComponent)c).setBorder(new javax.swing.border.LineBorder(LINE,line.getThickness(),line.getRoundedCorners()));
            }
        }
        if (c instanceof JTextComponent) {
            JTextComponent text = (JTextComponent) c;
            text.setCaretColor(FG); text.setSelectionColor(SELECT); text.setSelectedTextColor(FG);
            text.setDisabledTextColor(MUTED);
        }
        if (c instanceof RSyntaxTextArea && !snapshot.syntaxApplied) {
            RSyntaxTextArea editor = (RSyntaxTextArea) c;
            editor.setSyntaxScheme((SyntaxScheme) snapshot.syntax.clone());
            editor.setFont(snapshot.editorFont);
            applySyntax(editor); snapshot.syntaxApplied = true;
        }
        if (c instanceof Gutter) {
            Gutter gutter = (Gutter) c;
            gutter.setBackground(INPUT); gutter.setLineNumberColor(MUTED); gutter.setBorderColor(LINE);
            gutter.setFoldBackground(BG); gutter.setFoldIndicatorForeground(MUTED);
            gutter.setActiveLineRangeColor(SELECT);
        }
        if (c instanceof JTable && snapshot.table != null) snapshot.table.install((JTable) c, this);
        if (c instanceof JTree && snapshot.tree != null) snapshot.tree.install((JTree)c);
        if (c instanceof JMenu) patchTree(((JMenu) c).getPopupMenu());
        if (c instanceof Container) for (Component child : ((Container) c).getComponents()) patchTree(child);
        c.repaint();
    }

    private void replaceMenuBar(JFrame frame) {
        JMenuBar old = frame.getJMenuBar();
        if (old == null || menuBars.containsKey(frame)) return;
        // SoapUI's anonymous JMenuBar paintComponent always paints white. Preserve its menus and actions.
        JMenuBar replacement = new JMenuBar();
        replacement.setBorder(old.getBorder());
        while (old.getComponentCount() > 0) replacement.add(old.getComponent(0));
        menuBars.put(frame, old); frame.setJMenuBar(replacement); frame.revalidate();
    }

    private void restoreMenuBars() {
        menuBars.forEach((frame, original) -> {
            JMenuBar replacement = frame.getJMenuBar();
            if (replacement != null && replacement != original)
                while (replacement.getComponentCount() > 0) original.add(replacement.getComponent(0));
            frame.setJMenuBar(original); frame.revalidate();
        });
        menuBars.clear();
    }

    private static boolean neutral(Color c) {
        if (c == null) return false;
        return Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue()))
            - Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue())) < 45;
    }

    private static Color readable(Color c) {
        if (c == null || neutral(c)) return FG;
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return Color.getHSBColor(hsb[0], Math.min(hsb[1], .60f), Math.max(hsb[2], .88f));
    }

    private static void adaptColors(Component c) {
        Color background = c.getBackground();
        if (background != null && neutral(background)) {
            c.setBackground(c instanceof JTextComponent || c instanceof JTable || c instanceof JTree
                || c instanceof JList || c instanceof JViewport ? INPUT : BG);
            c.setForeground(readable(c.getForeground()));
        } else if (c.getForeground() != null && neutral(c.getForeground()) && background != null
            && (background.getRed() + background.getGreen() + background.getBlue()) < 390) c.setForeground(FG);
    }

    private static void applySyntax(RSyntaxTextArea editor) {
        SyntaxScheme scheme = (SyntaxScheme) editor.getSyntaxScheme().clone();
        for (int i = 0; i < scheme.getStyleCount(); i++) {
            Style style = scheme.getStyle(i);
            if (style != null) { style.foreground = readable(style.foreground); style.background = null; }
        }
        int[] comments = {TokenTypes.COMMENT_EOL, TokenTypes.COMMENT_MULTILINE, TokenTypes.COMMENT_DOCUMENTATION};
        int[] keywords = {TokenTypes.RESERVED_WORD, TokenTypes.RESERVED_WORD_2, TokenTypes.DATA_TYPE, TokenTypes.MARKUP_TAG_NAME, TokenTypes.MARKUP_TAG_DELIMITER};
        int[] strings = {TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, TokenTypes.LITERAL_CHAR, TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE};
        for (int token : comments) setToken(scheme, token, new Color(0x99ad8a));
        for (int token : keywords) setToken(scheme, token, new Color(0x8bbcff));
        for (int token : strings) setToken(scheme, token, new Color(0xe2b891));
        setToken(scheme, TokenTypes.MARKUP_TAG_ATTRIBUTE, new Color(0xc3a6ee));
        editor.setSyntaxScheme(scheme); editor.setBackground(INPUT); editor.setForeground(FG);
        editor.setCurrentLineHighlightColor(new Color(0x2c323c));
        editor.setMarginLineColor(LINE); editor.setMatchedBracketBGColor(SELECT);
        editor.setMatchedBracketBorderColor(MUTED); editor.setMarkOccurrencesColor(SELECT);
        editor.setHyperlinkForeground(new Color(0x8bbcff)); editor.setTabLineColor(LINE);
        for (int i = 0; i < editor.getSecondaryLanguageCount(); i++) editor.setSecondaryLanguageBackground(i + 1, BG);
    }

    private static void setToken(SyntaxScheme scheme, int token, Color color) {
        Style style = scheme.getStyle(token);
        if (style != null) style.foreground = color;
    }

    private static final class Snapshot {
        final Color background, foreground;
        final Border border;
        final Color caret, selection, selectedText, disabledText;
        final SyntaxScheme syntax;
        final Color[] syntaxColors, secondaryColors;
        final Font editorFont;
        final boolean bornDark;
        final Color[] gutter;
        final TableSnapshot table;
        final TreeSnapshot tree;
        final Icon icon,disabledIcon;
        final javax.swing.plaf.ScrollBarUI scrollUI;
        final boolean opaque;
        boolean syntaxApplied;

        Snapshot(Component c, boolean bornDark) {
            background = c.getBackground(); foreground = c.getForeground();
            border = c instanceof JComponent ? ((JComponent) c).getBorder() : null;
            opaque = c instanceof JComponent && ((JComponent)c).isOpaque();
            this.bornDark = bornDark && (BG.equals(background) || INPUT.equals(background));
            JTextComponent t = c instanceof JTextComponent ? (JTextComponent) c : null;
            caret = t == null ? null : t.getCaretColor(); selection = t == null ? null : t.getSelectionColor();
            selectedText = t == null ? null : t.getSelectedTextColor(); disabledText = t == null ? null : t.getDisabledTextColor();
            if (c instanceof RSyntaxTextArea) {
                RSyntaxTextArea editor = (RSyntaxTextArea) c;
                // RSyntaxTextArea 2.5.0 has zero-based getters and one-based setters.
                // Its Theme(RSyntaxTextArea) constructor gets this wrong and throws; snapshot explicitly.
                syntax = (SyntaxScheme) editor.getSyntaxScheme().clone();
                syntaxColors = new Color[]{editor.getCurrentLineHighlightColor(), editor.getMarginLineColor(),
                    editor.getMatchedBracketBGColor(), editor.getMatchedBracketBorderColor(), editor.getMarkOccurrencesColor(),
                    editor.getHyperlinkForeground(), editor.getTabLineColor()};
                secondaryColors = new Color[editor.getSecondaryLanguageCount()];
                for (int i = 0; i < secondaryColors.length; i++) secondaryColors[i] = editor.getSecondaryLanguageBackground(i);
            } else { syntax = null; syntaxColors = null; secondaryColors = null; }
            editorFont = c.getFont();
            scrollUI=c instanceof JScrollBar ? ((JScrollBar)c).getUI() : null;
            icon=c instanceof AbstractButton ? ((AbstractButton)c).getIcon() : c instanceof JLabel ? ((JLabel)c).getIcon() : null;
            disabledIcon=c instanceof AbstractButton ? ((AbstractButton)c).getDisabledIcon() : c instanceof JLabel ? ((JLabel)c).getDisabledIcon() : null;
            if (c instanceof Gutter) {
                Gutter g = (Gutter) c;
                gutter = new Color[]{g.getLineNumberColor(), g.getBorderColor(), g.getFoldBackground(),
                    g.getFoldIndicatorForeground(), g.getActiveLineRangeColor()};
            } else gutter = null;
            table = c instanceof JTable ? new TableSnapshot((JTable) c, this.bornDark) : null;
            tree = c instanceof JTree ? new TreeSnapshot((JTree)c,this.bornDark) : null;
        }

        void restore(Component c) {
            if (c instanceof com.eviware.soapui.support.swing.GradientPanel) ((JComponent)c).setOpaque(opaque);
            if(scrollUI!=null && !bornDark)((JScrollBar)c).setUI(scrollUI);
            if(c instanceof AbstractButton) { ((AbstractButton)c).setIcon(icon);((AbstractButton)c).setDisabledIcon(disabledIcon); }
            else if(c instanceof JLabel) { ((JLabel)c).setIcon(icon);((JLabel)c).setDisabledIcon(disabledIcon); }
            if(tree!=null)tree.restore((JTree)c);
            if (syntax != null) {
                RSyntaxTextArea editor = (RSyntaxTextArea) c;
                editor.setSyntaxScheme((SyntaxScheme) syntax.clone()); editor.setFont(editorFont);
                editor.setCurrentLineHighlightColor(syntaxColors[0]); editor.setMarginLineColor(syntaxColors[1]);
                editor.setMatchedBracketBGColor(syntaxColors[2]); editor.setMatchedBracketBorderColor(syntaxColors[3]);
                editor.setMarkOccurrencesColor(syntaxColors[4]); editor.setHyperlinkForeground(syntaxColors[5]);
                editor.setTabLineColor(syntaxColors[6]);
                for (int i = 0; i < secondaryColors.length; i++) editor.setSecondaryLanguageBackground(i + 1, secondaryColors[i]);
            }
            if (!bornDark) {
                c.setBackground(background); c.setForeground(foreground);
                if (c instanceof JComponent) ((JComponent) c).setBorder(border);
            } else {
                // A component opened at night has no daytime snapshot: use the restored L&F defaults.
                String prefix = c instanceof JTextComponent ? "TextArea" : c instanceof JTable ? "Table"
                    : c instanceof JTree ? "Tree" : c instanceof JList ? "List" : c instanceof JLabel ? "Label" : "Panel";
                c.setBackground(UIManager.getColor(prefix + ".background"));
                c.setForeground(UIManager.getColor(prefix + ".foreground"));
            }
            if (c instanceof com.eviware.soapui.support.swing.GradientPanel) {
                c.setForeground(foreground);
                c.setBackground(bornDark ? UIManager.getColor("control") : background);
            }
            if (c instanceof JTextComponent) {
                JTextComponent t = (JTextComponent) c;
                t.setCaretColor(bornDark ? UIManager.getColor("TextArea.caretForeground") : caret);
                t.setSelectionColor(bornDark ? UIManager.getColor("TextArea.selectionBackground") : selection);
                t.setSelectedTextColor(bornDark ? UIManager.getColor("TextArea.selectionForeground") : selectedText);
                t.setDisabledTextColor(bornDark ? UIManager.getColor("TextArea.inactiveForeground") : disabledText);
            }
            if (gutter != null) {
                Gutter g = (Gutter) c;
                g.setLineNumberColor(bornDark ? Color.GRAY : gutter[0]); g.setBorderColor(bornDark ? Color.LIGHT_GRAY : gutter[1]);
                g.setFoldBackground(bornDark ? Color.WHITE : gutter[2]); g.setFoldIndicatorForeground(bornDark ? Color.GRAY : gutter[3]);
                g.setActiveLineRangeColor(bornDark ? Gutter.DEFAULT_ACTIVE_LINE_RANGE_COLOR : gutter[4]);
            }
            c.repaint();
        }
    }

    private static final class TreeSnapshot {
        final javax.swing.tree.TreeCellRenderer original;
        final Color[] colors;
        final boolean bornDark;
        boolean installed;
        TreeSnapshot(JTree tree,boolean bornDark) {
            original=tree.getCellRenderer();this.bornDark=bornDark;
            if(original instanceof javax.swing.tree.DefaultTreeCellRenderer) {
                javax.swing.tree.DefaultTreeCellRenderer r=(javax.swing.tree.DefaultTreeCellRenderer)original;
                colors=new Color[]{r.getBackgroundNonSelectionColor(),r.getBackgroundSelectionColor(),
                    r.getTextNonSelectionColor(),r.getTextSelectionColor(),r.getBorderSelectionColor()};
            }else colors=null;
        }
        void install(JTree tree) {
            if(installed || original==null)return;installed=true;
            if(colors!=null) {
                javax.swing.tree.DefaultTreeCellRenderer r=(javax.swing.tree.DefaultTreeCellRenderer)original;
                r.setBackgroundNonSelectionColor(INPUT);r.setBackgroundSelectionColor(SELECT);
                r.setTextNonSelectionColor(FG);r.setTextSelectionColor(FG);r.setBorderSelectionColor(LINE);
            }
            tree.setCellRenderer((t,value,selected,expanded,leaf,row,focus) -> {
                Component c=original.getTreeCellRendererComponent(t,value,selected,expanded,leaf,row,focus);
                adaptColors(c);
                if(c instanceof JLabel) { JLabel label=(JLabel)c;label.setIcon(ContrastIcon.wrap(label.getIcon(),!label.isEnabled())); }
                return c;
            });
        }
        void restore(JTree tree) {
            tree.setCellRenderer(original);
            if(colors!=null) {
                javax.swing.tree.DefaultTreeCellRenderer r=(javax.swing.tree.DefaultTreeCellRenderer)original;
                r.setBackgroundNonSelectionColor(bornDark?UIManager.getColor("Tree.textBackground"):colors[0]);
                r.setBackgroundSelectionColor(bornDark?UIManager.getColor("Tree.selectionBackground"):colors[1]);
                r.setTextNonSelectionColor(bornDark?UIManager.getColor("Tree.textForeground"):colors[2]);
                r.setTextSelectionColor(bornDark?UIManager.getColor("Tree.selectionForeground"):colors[3]);
                r.setBorderSelectionColor(bornDark?UIManager.getColor("Tree.selectionBorderColor"):colors[4]);
            }
        }
    }

    private static final class TableSnapshot {
        final Map<Class<?>, TableCellRenderer> renderers = new LinkedHashMap<>();
        final Map<TableColumn, TableCellRenderer> columns = new LinkedHashMap<>();
        final Map<Component, Color[]> rendererColors = new java.util.IdentityHashMap<>();
        final Color grid, selectionBackground, selectionForeground;
        final boolean bornDark;
        boolean installed;
        TableSnapshot(JTable table, boolean bornDark) {
            this.bornDark = bornDark;
            grid = table.getGridColor(); selectionBackground = table.getSelectionBackground(); selectionForeground = table.getSelectionForeground();
            for (Class<?> type : new Class<?>[]{Object.class, Number.class, Boolean.class, java.util.Date.class, Icon.class})
                renderers.put(type, table.getDefaultRenderer(type));
            for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                TableColumn col = table.getColumnModel().getColumn(i); columns.put(col, col.getCellRenderer());
            }
            java.util.List<TableCellRenderer> all = new ArrayList<>(renderers.values()); all.addAll(columns.values());
            for (TableCellRenderer r : all) if (r instanceof Component) {
                Component c = (Component) r; rendererColors.put(c, new Color[]{c.getBackground(), c.getForeground()});
            }
        }
        void install(JTable table, ThemeEngine engine) {
            table.setGridColor(LINE); table.setSelectionBackground(SELECT); table.setSelectionForeground(FG);
            if (installed) return;
            installed = true;
            renderers.forEach((type, renderer) -> { if (renderer != null) table.setDefaultRenderer(type, wrap(renderer, engine)); });
            columns.forEach((col, renderer) -> { if (renderer != null) col.setCellRenderer(wrap(renderer, engine)); });
        }
        TableCellRenderer wrap(TableCellRenderer original, ThemeEngine engine) {
            RendererSurface surface = new RendererSurface();
            return (table, value, selected, focus, row, column) -> {
                Component cell = original.getTableCellRendererComponent(table, value, selected, focus, row, column);
                if (engine.dark) {
                    adaptColors(cell);
                    if (selected) { cell.setBackground(SELECT); cell.setForeground(FG); }
                    surface.configure(cell,selected);
                    return surface;
                }
                return cell;
            };
        }
        void restore(JTable table) {
            renderers.forEach(table::setDefaultRenderer); columns.forEach(TableColumn::setCellRenderer);
            table.setGridColor(bornDark ? UIManager.getColor("Table.gridColor") : grid);
            table.setSelectionBackground(bornDark ? UIManager.getColor("Table.selectionBackground") : selectionBackground);
            table.setSelectionForeground(bornDark ? UIManager.getColor("Table.selectionForeground") : selectionForeground);
            rendererColors.forEach((component, colors) -> {
                component.setBackground(bornDark ? null : colors[0]); component.setForeground(bornDark ? null : colors[1]);
            });
        }
    }

    /** SoapUI's PTable.prepareRenderer may overwrite colours after the delegate has returned. */
    private static final class RendererSurface extends JComponent {
        private Component renderer;
        private boolean selected;
        void configure(Component renderer,boolean selected) {
            this.renderer=renderer;this.selected=selected;
            setBackground(renderer.getBackground());setForeground(renderer.getForeground());
            setFont(renderer.getFont());setComponentOrientation(renderer.getComponentOrientation());
        }
        @Override public void setBackground(Color color) {
            super.setBackground(selected ? SELECT : color!=null && neutral(color) ? INPUT : color);
        }
        @Override public void setForeground(Color color) { super.setForeground(selected ? FG : readable(color)); }
        @Override public java.awt.Dimension getPreferredSize() { return renderer==null ? super.getPreferredSize() : renderer.getPreferredSize(); }
        @Override protected void paintComponent(java.awt.Graphics g) {
            if(renderer==null)return;
            renderer.setBackground(getBackground());renderer.setForeground(getForeground());
            SwingUtilities.paintComponent(g,renderer,this,0,0,getWidth(),getHeight());
        }
        @Override public String getToolTipText(java.awt.event.MouseEvent event) {
            return renderer instanceof JComponent ? ((JComponent)renderer).getToolTipText(event) : null;
        }
    }

    private static final class DarkPlasticTheme extends PlasticTheme {
        private ColorUIResource color(Color value) { return new ColorUIResource(value); }
        @Override public String getName() { return "SoapUI System Dark"; }
        @Override protected ColorUIResource getPrimary1() { return color(LINE); }
        @Override protected ColorUIResource getPrimary2() { return color(SELECT); }
        @Override protected ColorUIResource getPrimary3() { return color(BG); }
        @Override protected ColorUIResource getSecondary1() { return color(LINE); }
        @Override protected ColorUIResource getSecondary2() { return color(LINE); }
        @Override protected ColorUIResource getSecondary3() { return color(BG); }
        @Override protected ColorUIResource getBlack() { return color(FG); }
        @Override protected ColorUIResource getWhite() { return color(INPUT); }
        @Override public ColorUIResource getControlTextColor() { return color(FG); }
        @Override public ColorUIResource getUserTextColor() { return color(FG); }
        @Override public ColorUIResource getSystemTextColor() { return color(FG); }
        @Override public ColorUIResource getMenuForeground() { return color(FG); }
        @Override public ColorUIResource getMenuItemBackground() { return color(BG); }
        @Override public ColorUIResource getMenuItemSelectedBackground() { return color(SELECT); }
        @Override public ColorUIResource getMenuItemSelectedForeground() { return color(FG); }
    }
}
