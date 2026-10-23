# SoapUI System Theme 0.3.2

[**Download latest version (ZIP)**](https://github.com/VincentVonk/soapui-system-theme/releases/latest/download/SoapUI-System-Theme.zip) · [Releases and version history](https://github.com/VincentVonk/soapui-system-theme/releases/latest)

A Windows plugin for **SoapUI Open Source 5.10.0** that automatically follows the light or dark **Windows app theme**. The default setting is **Follow Windows**.

## Light and dark

The same demo project in SoapUI Open Source 5.10.0 on Windows 11. Click either screenshot to view it at full size.

| Light | Dark |
| --- | --- |
| [![SoapUI in light mode](https://raw.githubusercontent.com/VincentVonk/soapui-system-theme/main/docs/screenshots/soapui-light.png)](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/screenshots/soapui-light.png) | [![SoapUI in dark mode, including the Windows title bar](https://raw.githubusercontent.com/VincentVonk/soapui-system-theme/main/docs/screenshots/soapui-dark.png)](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/screenshots/soapui-dark.png) |

**Independent, unofficial plugin.** This project is not affiliated with, sponsored by, endorsed by, or maintained by SmartBear. SoapUI and SmartBear names and marks belong to their respective owners and are used to identify compatibility.

Original project code is provided under the [MIT license](LICENSE). SoapUI libraries retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Developed with assistance from OpenAI Codex/ChatGPT.

Download **SoapUI-System-Theme.zip** from GitHub Releases for a ready-to-install plugin. End users need Windows and an existing SoapUI Open Source 5.10.0 installation; no JDK or build is needed. The download links become available after the first non-draft, non-prerelease release is published with that asset name.

GitHub's automatic **Source code (zip)** and **Source code (tar.gz)** downloads contain source code, without a built plugin. Developers can clone the [source repository](https://github.com/VincentVonk/soapui-system-theme) and follow [Building from source](#building-from-source). See also the [GitHub setup instructions](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/GITHUB_SETUP.md).

## Installation (no administrator privileges required)

1. Download **SoapUI-System-Theme.zip** from [GitHub Releases](https://github.com/VincentVonk/soapui-system-theme/releases/latest). Extract the **entire ZIP** into a folder, preserving `dist/` and `licenses/`, then close SoapUI.
2. Press **Win+R** and open `%USERPROFILE%\.soapuios\plugins`. Create the directory if it does not exist.
3. Remove any previously installed `soapui-system-theme-*.jar`, such as version 0.3.1. Keep only one version of System Theme in this directory.
4. Copy `dist\soapui-system-theme-0.3.2.jar` into that directory.
5. Start SoapUI Open Source 5.10.0. A **Theme** menu appears at the top.
6. Leave **Theme > Follow Windows** selected.

Alternatively, after removing any older version, open PowerShell in the extracted directory and run:

```powershell
.\Install.ps1
```

If your execution policy blocks scripts, use the manual copy steps above. Developers who built from source can use the same installation steps with their local `dist/` directory.

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

New in 0.3.2: on **Windows 11 (build 22000 or later)**, the native Windows title bar of SoapUI and its decorated Java dialogs follows the plugin's dark/light mode too. Native minimize, maximize, restore, close, dragging and resizing remain managed by Windows. Light mode restores the captured native setting. Newly opened dialogs are included.

Default HTML text in read-only error/details dialogs now uses a readable light foreground in dark mode. The message document and text are retained, and Light restores the original display setting. Editable HTML request/response documents are excluded. HTML that explicitly specifies its own text colors can retain those colors.

The **Toggles to tab-based layout** button now uses a dark two-panel glyph in both its normal and disabled states. Its dimensions, button model and action are retained, and Light restores the original icons.

This uses Windows' DWM dark-mode attribute through an embedded, original Windows PowerShell helper. It runs asynchronously, without a profile or execution-policy change, and only targets decorated AWT windows belonging to the current SoapUI process. One helper runs while dark mode is active and exits after restoration or application shutdown. The first native update can take a few seconds. No Windows theme/accent settings or SoapUI launch files are changed, and no additional libraries are downloaded or bundled. If Windows or local security policy does not support the helper, the ordinary title bar is retained and the Swing theme still works. High-contrast settings, accent colors and other Windows customizations can affect the final native appearance.

Added in 0.3.1: the minimize, maximize, restore, and close icons on SoapUI's internal windows use dark backgrounds with light symbols. This also works after maximizing and restoring a window. The original buttons and actions are preserved; light mode restores the original icons.

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

## Limitations of version 0.3.2

This is an independent plugin, rather than an extension maintained by SmartBear. The build and tests use the actual SoapUI 5.10.0 libraries and its bundled Java 17 runtime. In addition to individual component tests, the actual SoapUI desktop is launched with a synthetic project and checked visually. Not every SoapUI screen or third-party plugin has been tested individually.

Some legacy icons, charts, components with custom painting, native Windows dialogs, and other embedded web pages may retain their own colors. Windows draws the native title bars; the plugin requests dark mode for decorated Java windows on Windows 11 without replacing window decorations. Windows 10 retains its ordinary native title bars. Logos and images in web pages are not recolored. Colors with specific meanings, such as status colors, are preserved wherever possible. You can always switch back to **Light**.

Endpoint Explorer and the local offline start page have been tested with actual JavaFX rendering. The online start page is supported through its known SoapUI URL, but could not be loaded over the internet in this test environment, so it has not been fully validated visually. Changes to that page or URL by SmartBear may require additional styling. A light appearance may briefly be visible during startup or page loading before the plugin or stylesheet becomes active.

The plugin targets Windows and requires SoapUI Open Source 5.10.0 or later according to its plugin metadata. **Only 5.10.0 has been validated.** Compatibility with later versions needs to be checked again. ReadyAPI is not separately supported.

## Uninstalling

Close SoapUI, remove `%USERPROFILE%\.soapuios\plugins\soapui-system-theme-0.3.2.jar`, and start SoapUI again. You can also use `.\Uninstall.ps1`. Only this plugin's JAR is removed. The small preferences file is retained; you may also remove `system-theme.properties`.

## Building from source

Requirements: Windows, a desktop session, a **JDK 17 or newer**, and SoapUI Open Source 5.10.0. Maven and internet access are not required.

```powershell
.\build.ps1 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.10.0'
```

The build tries to detect the JDK and SoapUI automatically; running just `.\build.ps1` is often sufficient. If needed, pass `-JdkHome` with the directory of your installed JDK. `SoapUiHome` is the directory containing `bin` and `lib`. An existing installation may keep an older directory name after an upgrade, so the build checks for `bin\soapui-5.10.0.jar`.

The build creates `dist\soapui-system-theme-0.3.2.jar`. The `--release 17` option ensures compatibility with SoapUI's Java 17. Tests use SoapUI's own JRE and work in `build\test-home` and `build\ui-home`; they do not change your actual Windows theme or SoapUI projects. Most graphical tests are rendered offscreen. On Windows 11, the native-title-bar checks briefly show test windows without requesting keyboard focus and capture their actual compositor output; Swing's offscreen printing cannot verify a native DWM title bar. The actual desktop test uses a temporary preferences profile and a test implementation of Java Preferences, so your user registry is not used for Java preferences. SoapUI's legacy JGoodies version requires a desktop session, even when rendering to an image.

## Binary release contents

- `dist/soapui-system-theme-0.3.2.jar`: the tested, installable plugin. Keep this path intact for `Install.ps1`.
- `Install.ps1`, `Uninstall.ps1`: optional installation helpers.
- `README.md`: installation, use, and limitations.
- `LICENSE`, `THIRD_PARTY_NOTICES.md`, `licenses/`: project license and separate library notices.

The binary ZIP contains these items directly at its root, with no extra wrapper directory. It contains no SoapUI/JDK libraries, Java source tree, test profiles, or screenshots. The original Windows helper is embedded in the plugin JAR, alongside the license notices under `META-INF/`.

The source repository additionally contains `src/main/java/`, `src/test/java/`, `build.ps1`, `test-ui.ps1`, `release.ps1`, packaging checks in `tests/`, and [TEST-REPORT.md](https://github.com/VincentVonk/soapui-system-theme/blob/main/TEST-REPORT.md). Generated `build/`, `dist/`, and `releases/` directories are ignored by Git. See [CHANGELOG.md](https://github.com/VincentVonk/soapui-system-theme/blob/main/CHANGELOG.md) for version history.

## Preparing a manual release (maintainers)

From the source checkout, with the build requirements above installed, run:

```powershell
.\release.ps1 -Version 0.3.2
# If automatic detection fails:
.\release.ps1 -Version 0.3.2 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.10.0' -JdkHome 'C:\Program Files\Java\jdk-27.0.1.0.2'
```

`-Version` is an optional assertion; the script reads the version from `SystemThemePlugin.java`. It checks the build/install/uninstall/UI-test JAR references, README heading, changelog, controller startup message and dependency-notice version. For a future version, update these references together, including other user-facing version strings in `ThemeController.java` and `Uninstall.ps1`.

Every run calls `build.ps1` with **all tests enabled**, including the actual SoapUI desktop integration test. There is no `-SkipTests` option. The build cleans compiled classes and copied notices before compiling. Only after a successful build, tests, license-content checks, and ZIP path/content checks does the script write:

```text
releases/SoapUI-System-Theme.zip
```

The original `dist/soapui-system-theme-0.3.2.jar` is retained. A previous release ZIP is removed at the start of a new attempt, so a failed attempt does not leave an older ZIP ready to upload. The ZIP name stays fixed for every version; its JAR remains versioned. The script prints the ZIP's SHA-256 for reference. Repeated builds use the same process and layout; archive timestamps may differ.

Run the isolated packaging regression checks separately with `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\release.Tests.ps1`. These use a synthetic build fixture; they do not replace the real build/tests performed by `release.ps1`.

To publish release **v0.3.2** yourself:

1. Review the local changes, run `release.ps1` successfully, and check the extracted ZIP. You can try `Install.ps1`/`Uninstall.ps1` with `-PluginDirectory` pointing to a temporary directory.
2. Commit and push the reviewed source changes yourself. Keep generated JARs/ZIPs out of the source commit. If you change any source or packaged documentation after testing, rebuild the release ZIP.
3. Open the repository's [Releases page](https://github.com/VincentVonk/soapui-system-theme/releases), choose **Draft a new release**, create tag **v0.3.2** against the commit just pushed, and title it **SoapUI System Theme 0.3.2**. If that tag already exists, verify that it points to the intended commit.
4. Add release notes from the changelog, state compatibility with Windows / SoapUI Open Source 5.10.0, and attach **releases/SoapUI-System-Theme.zip** as a release asset. Keep the asset name exactly **SoapUI-System-Theme.zip**.
5. Review the draft, publish it as a full release (not a prerelease), and select it as the latest release. Check both README download links and download/extract the uploaded asset once.

The release script never commits, tags, pushes, installs, or publishes. [GitHub documents the stable latest-release and asset links here](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases).

## Technical references

- [Microsoft: native DWM dark-mode attribute](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute)
- [SoapUI 5.10.0 plugin loader](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/PluginLoader.java)
- [SoapUI plugin interface](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/Plugin.java)
- [SoapUI default theme](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/StandaloneSoapUICore.java)
- [SoapUI interface and hardcoded white backgrounds](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/SoapUI.java)
- [SoapUI dialog headings](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/DescriptionPanel.java)
- [JavaFX browser component](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/components/EnabledWebViewBasedBrowserComponent.java)

No SoapUI or other third-party JARs are distributed with this package.

## Publishing and contributing

Keep source code in Git and distribute the installable ZIP through GitHub Releases using the process above. Keep LICENSE, THIRD_PARTY_NOTICES.md, and licenses/ with both distributions.

For changes, build and run the tests with build.ps1. When reporting UI issues, include your SoapUI version, Java version, theme setting, and affected screen. Use synthetic examples without credentials or actual request data.
