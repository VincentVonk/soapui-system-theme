# SoapUI System Theme 0.3.2

**Dark mode for SoapUI Open Source, without rebuilding SoapUI.**
Automatically follow the Windows **application theme**, or choose **Light** or **Dark** from the Theme menu.
Built and validated with **SoapUI Open Source 5.10.0**.

## Download

[**Download Latest Version — SoapUI-System-Theme.zip**](https://github.com/VincentVonk/soapui-system-theme/releases/latest/download/SoapUI-System-Theme.zip)
· [Browse GitHub Releases](https://github.com/VincentVonk/soapui-system-theme/releases)

The binary ZIP is ready to install: **no JDK, Maven or build environment is needed**.
GitHub's automatic **Source code (zip)** and **Source code (tar.gz)** archives contain source code, without a built plugin.

## Screenshots

The same demo project in Light and Dark on Windows 11. Click an image for the full-resolution screenshot.

| Light | Dark |
| --- | --- |
| [![SoapUI in light mode](https://raw.githubusercontent.com/VincentVonk/soapui-system-theme/main/docs/screenshots/soapui-light.png)](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/screenshots/soapui-light.png) | [![SoapUI in dark mode, including the Windows title bar](https://raw.githubusercontent.com/VincentVonk/soapui-system-theme/main/docs/screenshots/soapui-dark.png)](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/screenshots/soapui-dark.png) |

## Features

- Follow the Windows application theme automatically, including changes made by an existing theme scheduler.
- Override it with manual **Light** and **Dark** modes.
- Switch themes while SoapUI is open, without restarting it.
- Dark styling for supported toolbars, menus, dialogs, trees, tables and editor components.
- XML, JSON, Groovy and SQL editor styling, including syntax colors, caret, selection and line numbers.
- JavaFX styling for Endpoint Explorer and known SoapUI start pages, within the limits below.
- Native dark title bars for supported decorated Java windows on **Windows 11**.
- Restore the normal SoapUI appearance through **Light (SoapUI default)**.
- Install in your user profile, without changing SoapUI installation files or Windows theme settings.

## Installation

No administrator privileges are required for the default installation directory.

1. Download **SoapUI-System-Theme.zip** using the link above.
2. Extract the **entire archive**, keeping `dist/`, the scripts and license files together.
3. Close SoapUI.
4. Remove any older `soapui-system-theme-*.jar` from `%USERPROFILE%\.soapuios\plugins`. Keep only one version installed.
5. Copy `dist\soapui-system-theme-0.3.2.jar` into `%USERPROFILE%\.soapuios\plugins`. Create the directory if needed.
6. Start SoapUI Open Source 5.10.0 and select **Theme > Follow Windows**.

Alternatively, after closing SoapUI and removing any older plugin version, open PowerShell in the extracted folder:

```powershell
.\Install.ps1
```

The script uses the same user plugin directory and refuses installation alongside a different plugin version.
If script execution is restricted, use the manual copy steps above.
Restart SoapUI after installation; ordinary theme switches do not need a restart.

## Compatibility

| Environment | Status |
| --- | --- |
| Windows 10 | Windows app-theme support is implemented; not separately validated in the recorded test runs. Native title bars retain their ordinary appearance. |
| Windows 11 | Validated on build 26100. Native dark title bars require build 22000 or later and an available Windows helper. |
| SoapUI Open Source 5.10.0 | Validated with its bundled Java 17 runtime. |
| Later SoapUI versions | Not validated; the metadata's minimum-version setting is not a compatibility guarantee. |
| ReadyAPI | Not separately supported. |

The plugin uses libraries supplied by your SoapUI installation. No SoapUI or third-party JARs are bundled.

## Configuration

Use the **Theme** menu:

| Setting | Behavior |
| --- | --- |
| Follow Windows | Default. Follows the Windows application theme, checked approximately every three seconds. |
| Light (SoapUI default) | Restores the original light appearance and ignores the Windows theme. |
| Dark | Keeps supported components dark, regardless of the Windows theme. |

Manual Light/Dark modes pause Windows theme checks; returning to Follow Windows requests a fresh reading.
Your choice is saved across restarts in `%USERPROFILE%\.soapuios\system-theme.properties`.
An existing Windows theme scheduler controls switching times; the plugin does not create a schedule.
If Windows' preference is temporarily unreadable, the current appearance is retained while the plugin retries.

## Known limitations

- Some legacy/custom-painted components, charts, images, native Windows dialogs and third-party plugins may retain their own colors.
- JavaFX support is limited to Endpoint Explorer and known SoapUI start pages. Other pages, including OAuth login pages, are outside this support.
- Endpoint Explorer and the bundled offline start page were rendered in tests. The live online start page has not been fully validated visually.
- Native title-bar styling is limited to supported decorated Java windows on Windows 11. Accent colors, high-contrast settings or local restrictions can affect it.
- A brief light appearance can occur during startup or page loading. The first native title-bar update may take a few seconds.
- Not every SoapUI screen or third-party plugin has been tested. HTML with explicit text colors can retain those colors.

See the [technical notes and limitations](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/DEVELOPMENT.md#technical-limitations) for details and the scope of validation.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| The Theme menu does not appear | Use SoapUI Open Source 5.10.0, confirm the JAR is in `%USERPROFILE%\.soapuios\plugins`, and restart SoapUI. |
| Several plugin versions are installed | Close SoapUI and keep only the intended `soapui-system-theme-*.jar`. The installer refuses a different version alongside it. |
| Dark mode does not follow Windows | Select **Follow Windows**, change the Windows **application** theme and allow a few seconds. Changing only the taskbar/system theme is insufficient. Try manual **Dark** to check the plugin separately. |
| The native title bar stays light | Windows 10 keeps ordinary title bars. On Windows 11, allow the first update a few seconds; unsupported windows or a restricted helper may retain their original frame. |
| PowerShell or security policy blocks execution | Install by copying the JAR manually. If the native title-bar helper is blocked, the Swing theme can still work. Follow your organization's policy. |
| You want the original appearance | Select **Light (SoapUI default)**. To remove the plugin, use the uninstall steps below. |

To uninstall, close SoapUI, remove `%USERPROFILE%\.soapuios\plugins\soapui-system-theme-0.3.2.jar`, and restart it.
You can also run `.\Uninstall.ps1` from the extracted folder. The saved preference is retained;
remove `system-theme.properties` separately only if you also want to clear it.

[Report an issue](https://github.com/VincentVonk/soapui-system-theme/issues) with your SoapUI/Java versions, theme setting and affected screen.
Use a synthetic example without credentials or real request data.

## For developers

- [Building, testing and technical architecture](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/DEVELOPMENT.md)
- [Test reports and validation limits](https://github.com/VincentVonk/soapui-system-theme/blob/main/TEST-REPORT.md)
- [Preparing a manual release](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/RELEASING.md)
- [Changelog](https://github.com/VincentVonk/soapui-system-theme/blob/main/CHANGELOG.md)
- [Repository setup](https://github.com/VincentVonk/soapui-system-theme/blob/main/docs/GITHUB_SETUP.md)

## License and acknowledgements

Original project code is copyright (c) 2026 Vincent Vonk and available under the [MIT license](LICENSE).
SoapUI and its libraries retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
Development was assisted by **OpenAI Codex/ChatGPT**.

**Independent, unofficial plugin.** Not affiliated with, sponsored by, endorsed by or maintained by SmartBear.
SoapUI and SmartBear names and marks belong to their respective owners and identify compatibility.
