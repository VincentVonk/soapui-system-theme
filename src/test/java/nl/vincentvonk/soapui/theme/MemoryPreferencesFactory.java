// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/** Test-only Preferences store: the real SoapUI desktop must not write the user's registry. */
public final class MemoryPreferencesFactory implements PreferencesFactory {
    private final Preferences root = new Node(null, "");
    public Preferences userRoot() { return root; }
    public Preferences systemRoot() { return root; }
    private static final class Node extends AbstractPreferences {
        private final Map<String,String> values = new HashMap<>();
        private final Map<String,Node> children = new HashMap<>();
        Node(AbstractPreferences parent,String name) { super(parent,name); }
        protected void putSpi(String key,String value) { values.put(key,value); }
        protected String getSpi(String key) { return values.get(key); }
        protected void removeSpi(String key) { values.remove(key); }
        protected void removeNodeSpi() { values.clear(); children.clear(); }
        protected String[] keysSpi() { return values.keySet().toArray(new String[0]); }
        protected String[] childrenNamesSpi() { return children.keySet().toArray(new String[0]); }
        protected AbstractPreferences childSpi(String name) { return children.computeIfAbsent(name,k -> new Node(this,k)); }
        protected void syncSpi() { }
        protected void flushSpi() { }
    }
}
