# SoapUI System Theme 0.3.1

A Windows plugin for **SoapUI Open Source 5.10.0** that automatically follows the light or dark **Windows app theme**. The default setting is **Follow Windows**.

**Independent, unofficial plugin.** This project is not affiliated with, sponsored by, endorsed by, or maintained by SmartBear. SoapUI and SmartBear names and marks belong to their respective owners and are used to identify compatibility.

Original project code is provided under the [MIT license](LICENSE). SoapUI libraries retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Developed with assistance from OpenAI Codex/ChatGPT.

This GitHub package contains the source code. Build the plugin first with `build.ps1`; a JDK 17 or newer and an existing SoapUI 5.10.0 installation are required. For the initial upload and local clone, see the [GitHub setup instructions](docs/GITHUB_SETUP.md).

## Installation (no administrator privileges required)

1. Build the plugin with `build.ps1` (see [Building from source](#building-from-source)), then close SoapUI. If you are using a complete binary release, extract it first.
2. Press **Win+R** and open `%USERPROFILE%\.soapuios\plugins`. Create the directory if it does not exist.
3. Remove any previously installed `soapui-system-theme-*.jar`, such as version 0.3.0. Keep only one version of System Theme in this directory.
4. Copy `dist\soapui-system-theme-0.3.1.jar` into that directory.
5. Start SoapUI Open Source 5.10.0. A **Theme** menu appears at the top.
6. Leave **Theme > Follow Windows** selected.

Alternatively, run `.\Install.ps1` in PowerShell from the extracted directory. If your execution policy blocks scripts, use the manual copy steps above.

No SoapUI installation files or Windows settings are modified. The plugin does not download dependencies; it uses libraries already included with SoapUI.

## Day and night

Your existing Windows scheduler controls the switching times. Every **3 seconds**, the plugin reads `AppsUseLightTheme` from your user registry. A value of `0` means dark; `1` means light. It follows **the app theme**, rather than just the taskbar color or Windows system theme.

When Dark Mode X / Auto Dark Mode changes the Windows app theme, an open SoapUI window switches along with it. No restart is required. During startup, the default light interface may briefly appear before the first reading is available.

If the Windows setting cannot be read temporarily, SoapUI keeps its current appearance and the plugin retries. You can choose a theme manually through **Theme** in the meantime.

## Menu

| Option | Behavior |
|---|---|
| Follow Windows | Follows the Windows app theme. |
| Light (SoapUI default) | Restores the original SoapUI appearance and ignores the Windows app theme. |
| Dark | Keeps SoapUI dark even when Windows uses a light theme. |

Your choice is saved in `%USERPROFILE%\.soapuios\system-theme.properties`. The `mode` value is `SYSTEM`, `LIGHT`, or `DARK`. The file is read at the next startup; use the menu to switch immediately.

## What this version changes

New in 0.3.1: the minimize, maximize, restore, and close icons on SoapUI's internal windows use dark backgrounds with light symbols. This also works after maximizing and restoring a window. The original buttons and actions are preserved; light mode restores the original icons. This applies to internal SoapUI windows, rather than the native Windows title bar.

Added in 0.3.0:

- Dark, readable headings in Preferences, New SOAP Project, and other dialogs using SoapUI's DescriptionPanel. Its gradient internally uses the foreground color for painting; dark mode temporarily replaces that gradient with a solid background.
- A dark information panel in About SoapUI. Version information, copyright, build date, and hyperlinks are preserved. The white background image is omitted in dark mode. Light mode restores the original HTML document and image.
- A dark background and light blue question mark for the standard help icon. Its original action and dimensions are preserved; light mode restores the original icon.
- A separate dark JavaFX web stylesheet for Endpoint Explorer and the known online/local SoapUI start page. This does not change HTML documents, input values, or JavaScript actions, and does not reload the page. Light mode restores the original user stylesheet. Other web pages, such as OAuth login pages, are outside this support.

The improvements from 0.2.0 are retained: the main toolbar, navigator bar, inspector and log bars, and project overview bar receive a dark background despite SoapUI's hardcoded white painting. The original bars and buttons keep their actions and layout. Light mode removes the temporary painting layers.

Dark, neutral pixels in icons receive more contrast without modifying the source icon. Colored indicators, such as red and green symbols, retain their colors. Tables stay dark when SoapUI later tries to apply light row stripes. The tree renderer and scrollbars are also adjusted and restored in light mode.

- Standard Swing components: panels, menus, text fields, trees, tables, and dialog components.
- SoapUI's RSyntaxTextArea XML/JSON/Groovy/SQL editors: background, text, syntax colors, caret, selection, and line numbers.
- The white main menu bar painted by SoapUI: dark mode temporarily substitutes a standard Swing menu bar with the same menus and actions. Light mode restores the original bar.
- Newly opened Swing windows and components are also styled while dark mode is active.
- Existing request/response and script documents, caret selections, and project models are not rebuilt. Only the read-only About information panel temporarily uses a separate display document.

Light mode preserves existing component colors. Components created during dark mode receive the default light colors when switching back if no previous light state exists.

## Limitations of version 0.3.1

This is an independent plugin, rather than an extension maintained by SmartBear. The build and tests use the actual SoapUI 5.10.0 libraries and its bundled Java 17 runtime. In addition to individual component tests, the actual SoapUI desktop is launched with a synthetic project and checked visually. Not every SoapUI screen or third-party plugin has been tested individually.

Some legacy icons, charts, components with custom painting, native Windows dialogs, and other embedded web pages may retain their own colors. The native title bar is drawn by Windows/Java; the plugin does not replace window decorations. Logos and images in web pages are not recolored. Colors with specific meanings, such as status colors, are preserved wherever possible. You can always switch back to **Light**.

Endpoint Explorer and the local offline start page have been tested with actual JavaFX rendering. The online start page is supported through its known SoapUI URL, but could not be loaded over the internet in this test environment, so it has not been fully validated visually. Changes to that page or URL by SmartBear may require additional styling. A light appearance may briefly be visible during startup or page loading before the plugin or stylesheet becomes active.

The plugin targets Windows and requires SoapUI Open Source 5.10.0 or later according to its plugin metadata. **Only 5.10.0 has been validated.** Compatibility with later versions needs to be checked again. ReadyAPI is not separately supported.

## Uninstalling

Close SoapUI, remove `%USERPROFILE%\.soapuios\plugins\soapui-system-theme-0.3.1.jar`, and start SoapUI again. You can also use `.\Uninstall.ps1`. Only this plugin's JAR is removed. The small preferences file is retained; you may also remove `system-theme.properties`.

## Building from source

Requirements: Windows, a desktop session, a **JDK 17 or newer**, and SoapUI Open Source 5.10.0. Maven and internet access are not required.

```powershell
.\build.ps1 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.10.0'
```

The build tries to detect the JDK and SoapUI automatically; running just `.\build.ps1` is often sufficient. If needed, pass `-JdkHome` with the directory of your installed JDK. `SoapUiHome` is the directory containing `bin` and `lib`. An existing installation may keep an older directory name after an upgrade, so the build checks for `bin\soapui-5.10.0.jar`.

The build creates `dist\soapui-system-theme-0.3.1.jar`. The `--release 17` option ensures compatibility with SoapUI's Java 17. Tests use SoapUI's own JRE and work in `build\test-home` and `build\ui-home`; they do not change your actual Windows theme or SoapUI projects. For graphical regression tests, test windows are placed offscreen without keyboard focus and rendered to PNG. The actual desktop test uses a temporary preferences profile and a test implementation of Java Preferences, so your user registry is not used for Java preferences. SoapUI's legacy JGoodies version requires a desktop session, even when rendering to an image.

## Package contents

- `dist/`: the build creates the installable plugin JAR here; this directory is not included in the source package.
- `src/main/java/`: complete source code.
- `src/test/java/`: regression and integration tests.
- `build.ps1`: local build without downloads.
- `test-ui.ps1`: tests the actual SoapUI desktop with the built plugin and a separate test profile.
- `Install.ps1`, `Uninstall.ps1`: optional installation helpers.
- `TEST-REPORT.md`: checks performed and their limitations.
- `build/`: tests create temporary profiles and PNG images here; these files are excluded from Git and the source package.
- `LICENSE`, `THIRD_PARTY_NOTICES.md`, `licenses/`: project license and separate library notices.
- `.gitignore`, `.gitattributes`: exclusions and consistent line endings for Git.
- `docs/GITHUB_SETUP.md`, `CHANGELOG.md`: GitHub instructions and version history.

## Technical references

- [SoapUI 5.10.0 plugin loader](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/PluginLoader.java)
- [SoapUI plugin interface](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/Plugin.java)
- [SoapUI default theme](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/StandaloneSoapUICore.java)
- [SoapUI interface and hardcoded white backgrounds](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/SoapUI.java)
- [SoapUI dialog headings](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/DescriptionPanel.java)
- [JavaFX browser component](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/components/EnabledWebViewBasedBrowserComponent.java)

No SoapUI or other third-party JARs are distributed with this package.

## Publishing and contributing

Upload the extracted source files to the root of your repository. The ZIP contains no Git history, SoapUI installation files, or generated binaries. Keep LICENSE, THIRD_PARTY_NOTICES.md, and licenses/ with the source code. A locally built plugin JAR also includes these notices under META-INF/.

For changes, build and run the tests with build.ps1. When reporting UI issues, include your SoapUI version, Java version, theme setting, and affected screen. Use synthetic examples without credentials or actual request data.
