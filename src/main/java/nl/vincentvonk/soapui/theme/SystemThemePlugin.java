// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Entry point discovered by the unmodified SoapUI Open Source plugin loader. */
@PluginConfiguration(groupId = "nl.vincentvonk.soapui", name = "System Theme",
    version = "0.3.1", minimumSoapUIVersion = "5.10.0", autoDetect = false,
    description = "Follow Windows app light/dark mode; restore SoapUI's original light appearance.")
public final class SystemThemePlugin extends PluginAdapter {
    private static ThemeController controller;
    private static Timer startup;

    @Override public boolean isActive() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    @Override public void initialize() {
        // Do not start UI threads in testrunner, Maven, mock services or other headless uses.
        if (GraphicsEnvironment.isHeadless()) return;
        SwingUtilities.invokeLater(() -> {
            if (controller != null || startup != null) return;
            startup = new Timer(400, e -> {
                if (SoapUI.getFrame() == null || !SoapUI.getFrame().isShowing()) return;
                startup.stop();
                startup = null;
                controller = new ThemeController(SoapUI.getFrame());
                controller.start();
            });
            startup.start();
        });
    }
}
