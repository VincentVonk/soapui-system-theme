// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import java.io.StringReader;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/** Only SoapUI's informational About pane. Request/response editor documents are never replaced. */
final class AboutPaneTheme {
    private final Map<JEditorPane,Document> originals=new WeakHashMap<>();
    void install(JEditorPane pane) {
        if (originals.containsKey(pane) || pane.isEditable() || !(pane.getDocument() instanceof HTMLDocument)) return;
        java.awt.Window window=SwingUtilities.getWindowAncestor(pane);
        if (!(window instanceof JDialog) || !"About SoapUI".equals(((JDialog)window).getTitle())) return;
        String html=pane.getText();
        if (!html.contains("smartbear.com") || !html.replaceAll("\\s+", " ").contains("Build Date:")) return;
        // The supplied splash is a white image, with fixed black text over it. Use a plain dark
        // informational panel while retaining the actual version, copyright, links and build date.
        html=html.replaceAll("(?i)background-image\\s*:\\s*url\\([^)]*\\)\\s*;?", "")
            .replaceAll("(?i)color\\s*:\\s*black", "color:#e3e6eb")
            .replaceAll("(?i)margin-top\\s*:\\s*85px", "margin-top:24px");
        try {
            HTMLEditorKit kit=new HTMLEditorKit();
            HTMLDocument document=(HTMLDocument)kit.createDefaultDocument();
            document.getStyleSheet().addRule("body { background-color:#1d2025; color:#e3e6eb; }");
            document.getStyleSheet().addRule("a { color:#8bbcff; }");
            kit.read(new StringReader(html),document,0);
            originals.put(pane,pane.getDocument());
            pane.setDocument(document); pane.setCaretPosition(0);
        } catch (java.io.IOException | javax.swing.text.BadLocationException e) {
            java.util.logging.Logger.getLogger("SoapUI.SystemTheme").log(java.util.logging.Level.FINE,"About pane could not be styled",e);
        }
    }
    void restore() {
        originals.forEach((pane,document) -> { pane.setDocument(document); pane.setCaretPosition(0); });
        originals.clear();
    }
}
