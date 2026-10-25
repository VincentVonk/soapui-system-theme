// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package com.eviware.soapui.plugins;

import com.eviware.soapui.SoapUI;
import java.io.File;
import java.util.Collections;

/** Uses the actual SoapUI 5.10.0 scanner and classloader, with no plugin classes on the test classpath. */
public final class PluginLoadTest {
    public static void main(String[] args) throws Exception {
        try { run(args); System.exit(0); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    private static void run(String[] args) throws Exception {
        if (!"5.10.0".equals(SoapUI.SOAPUI_VERSION)) throw new AssertionError("Unexpected SoapUI version: " + SoapUI.SOAPUI_VERSION);
        PluginLoader loader = new PluginLoader(null, null, null);
        InstalledPluginRecord record = loader.loadPlugin(new File(args[0]), Collections.emptyList());
        if (!record.plugin.getInfo().getId().toString().contains("System Theme"))
            throw new AssertionError("Plugin metadata not loaded");
        if (!"nl.vincentvonk.soapui".equals(record.plugin.getInfo().getId().getGroupId()))
            throw new AssertionError("Published plugin identity changed during the namespace cleanup");
        // SoapUI may wrap the scanned entry point; inspect its JAR classloader.
        Class<?> entryPoint = record.pluginClassLoader.loadClass("soapui.systemtheme.SystemThemePlugin");
        if (entryPoint.getClassLoader() != record.pluginClassLoader)
            throw new AssertionError("Plugin entry point was not loaded from the packaged JAR");
        String expectedVersion = (String) entryPoint.getField("VERSION").get(null);
        try (var versionResource = entryPoint.getResourceAsStream("/META-INF/soapui-system-theme.version")) {
            if (versionResource == null || !expectedVersion.equals(new String(versionResource.readAllBytes(), java.nio.charset.StandardCharsets.US_ASCII).trim()))
                throw new AssertionError("Embedded binary version differs from central VERSION");
        }
        PluginConfiguration annotation = entryPoint.getAnnotation(PluginConfiguration.class);
        if (!expectedVersion.equals(annotation.version())) throw new AssertionError("Plugin annotation differs from central VERSION");
        Plugin instance = (Plugin) entryPoint.getDeclaredConstructor().newInstance();
        if (!record.plugin.hasSameIdAs(instance))
            throw new AssertionError("Discovered plugin differs from the project-namespace entry point");
        if (!record.plugin.isActive()) throw new AssertionError("Windows plugin not active");
        System.out.println("PASS: real SoapUI 5.10.0 JAR discovery, classloading, metadata and initialize() in headless mode");
    }
}
