// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package soapui.systemtheme;

import java.awt.Component;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.Timer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/** Optional JavaFX bridge. Only known SoapUI start pages receive a reversible user stylesheet. */
final class FxWebTheme {
    private static final String CSS="""
        html, body { background:#25282e !important; color:#e3e6eb !important; }
        body div, body section, body article, body main, body aside, body header, body footer,
        body table, body tbody, body tr, body td, body th, body form, body fieldset,
        .ui-widget-content, .ui-widget-header, .ui-tabs-panel {
          background-color:#25282e !important; color:#e3e6eb !important; border-color:#495361 !important;
        }
        body p, body span, body label, body li, body h1, body h2, body h3, body h4 {
          color:#e3e6eb !important;
        }
        a, a span { color:#8bbcff !important; }
        input, textarea, select, option, pre, code, [contenteditable=true] {
          background:#1d2025 !important; color:#e3e6eb !important; border-color:#495361 !important;
        }
        input::placeholder, textarea::placeholder { color:#a3acb9 !important; }
        button, input[type=button], input[type=submit], .ui-button, .ui-state-default {
          background:#303846 !important; color:#e3e6eb !important; border-color:#637184 !important;
        }
        button:hover, .ui-state-hover, .ui-state-active, .ui-tabs-active {
          background:#345a85 !important; color:#e3e6eb !important;
        }
        :disabled, .ui-state-disabled { color:#a3acb9 !important; }
        ::selection { background:#345a85; color:#e3e6eb; }
        ::-webkit-scrollbar { width:12px; height:12px; background:#25282e; }
        ::-webkit-scrollbar-thumb { background:#637184; border:2px solid #25282e; }
        """;
    private static final String STYLESHEET="data:text/css;base64,"+
        Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));
    // Swing owns panels/timer. JavaFX owns engines/listeners. Never wait across the two UI threads.
    private final Map<JFXPanel,Boolean> panels=new WeakHashMap<>();
    private final Map<WebEngine,State> engines=new WeakHashMap<>();
    private final Timer discovery=new Timer(700,e -> discover());
    private volatile boolean dark;
    void watch(Component component) {
        panels.put((JFXPanel)component,Boolean.TRUE);
        if (dark && !discovery.isRunning()) discovery.start();
        discover();
    }
    void setDark(boolean value) {
        if (dark==value) return;
        dark=value;
        if (value) { if (!panels.isEmpty()) { discovery.start(); discover(); } }
        else {
            discovery.stop();
            Platform.runLater(() -> {
                if (dark) return;
                engines.forEach((engine,state) -> state.restore(engine)); engines.clear();
            });
        }
    }
    private void discover() {
        if (!dark) return;
        JFXPanel[] current=panels.keySet().toArray(new JFXPanel[0]);
        if (current.length==0) { discovery.stop(); return; }
        Platform.runLater(() -> {
            if (!dark) return;
            for (JFXPanel panel:current) if (panel.getScene()!=null) visit(panel.getScene().getRoot());
        });
    }
    private void visit(Node node) {
        if (node instanceof WebView) {
            WebEngine engine=((WebView)node).getEngine();
            State state=engines.get(engine);
            if (state==null) { state=new State(engine); engines.put(engine,state); }
            state.apply(engine);
        }
        if (node instanceof Parent) for (Node child:((Parent)node).getChildrenUnmodifiable()) visit(child);
    }
    static boolean isSoapUiPage(String location) {
        if (location==null || location.isEmpty()) return false;
        for (String path:new String[]{"/starter-page/starter-page.html",
            "/com/eviware/soapui/explorer/soapui-pro-api-endpoint-explorer-starter-page.html"}) {
            java.net.URL resource=com.eviware.soapui.SoapUI.class.getResource(path);
            if (resource!=null && location.equals(resource.toExternalForm())) return true;
        }
        try {
            URI uri=URI.create(location);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && ("soapui.org".equalsIgnoreCase(uri.getHost()) || "www.soapui.org".equalsIgnoreCase(uri.getHost()))
                && "/Appindex/soapui-starterpage-new-2026.html".equals(uri.getPath());
        } catch (IllegalArgumentException e) { return false; }
    }
    private final class State {
        final String original;
        final ChangeListener<String> listener;
        State(WebEngine engine) {
            original=engine.getUserStyleSheetLocation();
            WeakReference<WebEngine> reference=new WeakReference<>(engine);
            listener=(property,oldLocation,newLocation) -> {
                WebEngine current=reference.get(); if (current!=null) apply(current);
            };
            engine.locationProperty().addListener(listener);
        }
        void apply(WebEngine engine) {
            String wanted=dark && isSoapUiPage(engine.getLocation()) ? STYLESHEET : original;
            if (!Objects.equals(wanted,engine.getUserStyleSheetLocation())) engine.setUserStyleSheetLocation(wanted);
        }
        void restore(WebEngine engine) {
            engine.locationProperty().removeListener(listener);
            if (Objects.equals(engine.getUserStyleSheetLocation(),STYLESHEET)) engine.setUserStyleSheetLocation(original);
        }
    }
}
