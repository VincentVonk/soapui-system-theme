# Development and technical notes

Technical documentation for SoapUI System Theme **0.3.2**.
For installation and everyday use, start with the [README](../README.md).
For distribution, use the [manual release guide](RELEASING.md).

## Development prerequisites

- Windows with an interactive desktop session.
- SoapUI Open Source **5.10.0**, including its `bin/`, `lib/` and bundled Java runtime.
- A **JDK 17 or newer** with `javac.exe`, `jar.exe` and `java.exe`. A JRE alone cannot compile the plugin.
- PowerShell to run the repository's build and test scripts.

The build uses installed SoapUI libraries; Maven and dependency downloads are not required.
Java is compiled with `--release 17` for SoapUI's Java 17 runtime.
Only SoapUI 5.10.0 has been validated. Later versions need separate checks, even though the plugin metadata specifies a minimum of 5.10.0.
ReadyAPI is not separately supported.

## Building from source

Run from the source checkout:

```powershell
.\build.ps1
```

If automatic detection fails, specify the installation directories:

```powershell
.\build.ps1 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.10.0' -JdkHome 'C:\Program Files\Java\jdk-27.0.1.0.2'
```

Use paths that exist on your machine. `SoapUiHome` is the directory containing `bin` and `lib`.
An upgraded installation can retain an older directory name; the build checks for `bin\soapui-5.10.0.jar`.
It tries `JAVA_HOME` and the supported Program Files locations for a JDK when `-JdkHome` is omitted.

The output is `dist\soapui-system-theme-0.3.2.jar`.
The build clears compiled classes, test classes and copied resources before compiling, so obsolete files cannot enter a later JAR.
It includes `LICENSE`, `THIRD_PARTY_NOTICES.md` and `licenses/` under `META-INF/`, without embedding host libraries.

`build.ps1` runs the full test suite by default. Its `-SkipTests` switch is for a compilation-only development build;
it does not establish release readiness. `release.ps1` has no equivalent skip option.

## Testing and validation

Run `build.ps1` without `-SkipTests` to compile, package and execute:

- Component, editor and repeated Light/Dark restoration checks.
- Legacy toolbar, icon, table, tree and scrollbar checks.
- Internal-window control rendering and minimize/maximize/restore/close action checks.
- Dialog headers, About content, help icons, read-only error text and the tab-layout glyph.
- Real bundled JavaFX Endpoint Explorer and offline start-page rendering.
- Asynchronous controller switching, unreadable OS preferences, manual overrides and saved settings.
- Windows 11 native-title-bar read-back, restoration, dialog and helper-lifecycle checks where supported.
- Discovery and initialization through SoapUI's actual plugin loader, with plugin classes absent from the test application classpath.
- The actual SoapUI desktop with the packaged JAR, a synthetic project and dialogs.

The build prefers SoapUI's bundled JRE for the Java checks. `test-ui.ps1`, which the full build calls,
launches the actual desktop with that bundled runtime and expects `jre\bin\java.exe`.
The desktop scenario can also be repeated after a complete build:

```powershell
.\test-ui.ps1 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.10.0'
```

Tests use `build/test-home` and `build/ui-home`, rather than the real user profile.
The full desktop test uses a test-only in-memory Java Preferences factory, avoiding user-registry preference writes.
It creates synthetic local content and does not send API requests or execute user test scripts.
Most rendering is offscreen. Windows 11 native-title-bar checks briefly show test windows without requesting keyboard focus
and capture compositor output, because Swing's `printAll` cannot verify DWM's native title bar.
SoapUI's legacy JGoodies library still requires a desktop session, even for offscreen rendering.

The web test starts with page scripts disabled and executes only the bundled UI scripts needed by the fixtures,
skipping Google Analytics. Its HTTP/2-loader workaround is test-only; the plugin changes no network setting.
See the [test report](../TEST-REPORT.md) for the original environment, exact checks, screenshots inspected and caveats.
Its dated entries are historical evidence, not a claim that those checks ran during every documentation update.
The later 0.3.2 entries record 435 explicit checks plus the controller/loader/desktop scenarios; the earlier 358/378 totals cover earlier scopes.

A useful manual acceptance check is to keep unsaved request text and a selection, let the existing Windows scheduler change
the app theme, and open another request. Return to Light and check appearance, text and selection. Allow approximately 3–5 seconds after an OS change.

## Source layout and plugin identity

| Location | Purpose |
| --- | --- |
| `src/main/java/soapui/systemtheme/` | Plugin entry point, controller, Swing/JavaFX styling and Windows integration. |
| `src/main/resources/soapui/systemtheme/` | Original embedded Windows title-bar helper. |
| `src/test/java/soapui/systemtheme/` | Regression and integration fixtures. |
| `src/test/java/com/eviware/soapui/plugins/` | Original test code exercising SoapUI's package-private loader API; excluded from the plugin JAR. |
| `tests/release.Tests.ps1` | Isolated packaging/installer checks using a synthetic build. |
| `build/` | Generated classes, resources, profiles, logs and test screenshots; ignored by Git. |
| `dist/` | Locally built, versioned plugin JAR; ignored by Git. |
| `releases/` | Generated binary ZIP; ignored by Git. |

The Java namespace is `soapui.systemtheme`. The published plugin group ID remains `nl.vincentvonk.soapui`, with the name `System Theme`,
so the namespace cleanup does not change the identity recognized by SoapUI.
The minimum SoapUI version is metadata, rather than proof of compatibility with every later version.

## Theme detection and switching

`ThemeController` adds the Theme menu, loads the saved mode and polls the Windows preference on a background worker.
Swing changes run on the event-dispatch thread. The plugin avoids UI initialization in headless use such as command-line runners.

`WindowsAppTheme` uses an absolute `reg.exe` path beneath `SystemRoot`, with a bounded query timeout. It reads, without modifying:

```text
HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize
AppsUseLightTheme
```

`0` means Dark; `1` means Light. The plugin follows the **application** theme, rather than only the taskbar color or system theme.
The ordinary polling interval is three seconds. Existing Windows schedulers, including Auto Dark Mode / Dark Mode X,
control the time of the OS transition; this plugin does not create schedules.
An unreadable preference retains the current appearance, logs a warning and is retried. Manual overrides remain available.

The mode is saved in `%USERPROFILE%\.soapuios\system-theme.properties` as `SYSTEM`, `LIGHT` or `DARK`.
It is read at the next startup; use the menu to change the active session immediately.
A short light appearance can precede the first Windows reading during startup.

## Swing styling and restoration

`ThemeEngine` applies the dark palette and records original colors, UI defaults, renderers and icons for restoration.
It styles standard panels, menus, fields, trees, tables and supported dialogs, and patches components opened while Dark is active.
Existing request/response and script documents and project models are not rebuilt to change their appearance.
The targeted editor/controller tests check document identity, text and editor-selection preservation; those checks do not establish coverage of every editor or third-party component.
Components created in Dark without a prior Light snapshot receive restored look-and-feel defaults when returning to Light.

Important adaptations include:

- SoapUI's main menu bar paints a fixed white background. Dark mode temporarily substitutes a standard Swing menu bar with the same menus/actions; Light restores the original bar.
- Main, navigator, inspector/log and project-overview toolbars use temporary Swing `JLayer` painting guards, preserving the actual bars, controls, actions and layout. Light removes those guards.
- Neutral dark icon pixels receive more contrast without modifying the source image. Colored status indicators retain their colors where possible.
- Tables resist later light striping from the host; tree renderers and scrollbars are styled and restored.
- RSyntaxTextArea XML/JSON/Groovy/SQL editors receive dark text, syntax colors, caret, selection, line numbers and related gutter styling.
- Internal-window minimize/maximize/restore/close glyphs use dark faces and light symbols, including after state transitions. Original buttons, listeners and actions remain in place.
- DescriptionPanel's gradient uses its foreground as a paint color. Dark mode makes the panel opaque with a solid background, then restores the original paint properties.
- The read-only About pane temporarily uses a separate dark display document without the white splash background, preserving version, copyright, build date and hyperlinks. Light restores the original document by identity.
- The standard help glyph and tab-layout glyph receive original dark replacements with preserved dimensions/actions; original icons return in Light.
- Default HTML text in read-only error/details views honors the component foreground. The document and text are retained and the display property is restored in Light. Editable HTML is excluded; explicit HTML colors can remain.

These are presentation changes. The error-message readability adjustment does not repair the exception reported by a dialog.

## JavaFX web styling

`FxWebTheme` is optional and watches supported `JFXPanel` components when used.
It applies a separate dark **user stylesheet** to Endpoint Explorer and known online/local SoapUI start pages.
It does not reload the page, rewrite its HTML, change input values or replace JavaScript actions.
Light restores the original user stylesheet; navigation outside the supported page scope removes the plugin stylesheet.

The actual bundled Endpoint Explorer and offline starter page have been rendered and checked.
The known online starter URL is allowed, but the live page could not be loaded in the recorded test environment;
its complete visual appearance is unverified. SmartBear changes to the URL or page can require styling updates.
Other pages, including OAuth login pages, are outside this support. Logos and images are not recolored.

## Native Windows title bars

On Windows 11 build **22000 or later**, `WindowsTitleBar` requests `DWMWA_USE_IMMERSIVE_DARK_MODE` through DWM.
The original flag is captured and restored, instead of forcing caption/text colors whose original values could not be read reliably in the checked environment.
Windows retains the native decorations, drag/resize behavior and minimize/maximize/restore/close controls.

The original embedded PowerShell helper:

- Uses the operating system's Windows PowerShell/.NET and `user32`/`dwmapi` APIs, without redistributing those components or downloading libraries.
- Runs asynchronously outside Swing's event thread, through an absolute executable path, without a PowerShell profile or execution-policy change.
- Communicates through a private stdin/stdout protocol and targets decorated AWT frames/dialogs belonging to the current Java PID.
- Runs while Dark is active; Light, application closure or normal pipe closure restores captured settings and exits the helper.
- Includes newly opened decorated Java dialogs and skips unsupported/disappearing windows without disabling the remaining native styling.

The first helper startup can take a few seconds. If the helper cannot start or is restricted by local policy,
the Swing theme can still work while native title bars retain their ordinary appearance.
Windows 10 follows the unsupported-build path and retains its ordinary native title bars.
The recorded native validation was on Windows 11 build 26100; Windows 10 was not separately checked visually.

## Technical limitations

Only SoapUI Open Source 5.10.0 has been validated. Test coverage does not establish support for every dialog, screen or third-party plugin.
Native Windows dialogs, some legacy/custom-painted components, charts and other embedded web pages can retain their own colors.
Images are not recolored; colors with semantic meaning are preserved where possible.
Accent colors, high-contrast settings and other OS customizations can affect native appearance.
Startup/page loading can briefly show the ordinary light appearance.

The test report's earlier general statements about light native title bars and offscreen-only testing describe the pre-0.3.2 scope.
Its later Windows-11 and readability sections record the expanded 0.3.2 behavior and compositor checks.
Historical test screenshots are generated under `build/`; the two selected README screenshots are committed under `docs/screenshots/`.
Do not interpret historical environment restrictions or counts as new validation of the current source checkout.

## Contributing and issue reports

Build with tests enabled before proposing code changes. Keep source and notices in Git; distribute binaries through GitHub Releases.
Include SoapUI version, Java version, theme setting and the affected screen when [reporting an issue](https://github.com/VincentVonk/soapui-system-theme/issues).
Use synthetic content without credentials or real request data.
For an initial repository upload or clone, see [GitHub setup](GITHUB_SETUP.md).

## Technical references

- [Microsoft: DWM window attributes](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute)
- [SoapUI 5.10.0 plugin loader](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/PluginLoader.java)
- [SoapUI plugin interface](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/plugins/Plugin.java)
- [SoapUI default theme](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/StandaloneSoapUICore.java)
- [SoapUI interface and fixed backgrounds](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/SoapUI.java)
- [SoapUI dialog headings](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/DescriptionPanel.java)
- [JavaFX browser component](https://github.com/SmartBear/soapui/blob/v5.10.0/soapui/src/main/java/com/eviware/soapui/support/components/EnabledWebViewBasedBrowserComponent.java)

Original project code is MIT-licensed. Keep [LICENSE](../LICENSE), [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)
and [licenses/](../licenses/) with source distributions. The project license does not replace the host libraries' own terms.
