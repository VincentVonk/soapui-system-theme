# Changelog

## 0.3.2 - 2026-10-23

- Make default HTML text in read-only error/details dialogs readable in dark mode, preserving message text/document identity and restoring the original display property in Light. Leave editable HTML request/response documents untouched.
- Replace the tab-based-layout toggle's white icon face with a dark glyph in normal and disabled states, preserving the original size, source image and button action; restore the original icons in Light.
- Apply Windows 11 native dark title bars to SoapUI and newly opened decorated Java dialogs, alongside the existing Swing theme.
- Preserve native window decorations and restore the captured DWM setting in Light mode and when the helper exits.
- Use an embedded, original Windows PowerShell helper scoped to the current Java process, without downloads, additional native libraries, global theme changes or SoapUI launcher changes. Run it off the Swing event thread and retain the ordinary title bar if unavailable.
- Add native read-back, three dark/light cycles, new-dialog, shutdown and actual compositor screenshot tests, including the packaged plugin in the real SoapUI desktop.
- Update installation/build/release references to 0.3.2. Windows 10 native title bars remain unchanged.

### Source namespace cleanup - 2026-10-25

- Use the project namespace soapui.systemtheme for Java classes, tests and embedded resources, with matching source directories.
- Update build/test entry points, resource paths and release-package validation to the new namespace.
- Retain version 0.3.2, the published plugin identity and existing behavior. No new release is created by this source cleanup.

### Source reliability improvements (25 October 2026)

- Preserve SoapUI's original menu bar and menus with a paint-only dark surface, keeping dynamic menu updates and project import working in Dark.
- Retry incomplete Light restoration without losing original snapshots; continue independent restoration steps after a failure.
- Retry transient native-helper failures on a later theme selection, while avoiding automatic retries for window events and unsupported environments.
- Pause Windows theme queries in manual modes and reject results from an earlier mode selection.
- Use a central source version constant and verify matching binary metadata; standalone installers retain the existing ZIP layout.
- Extend isolated regressions for actual SoapUI import, selection, menu actions, editor preservation, failed restoration and helper recovery.

These are source changes within 0.3.2; no new release is published by this change.

## 0.3.1 - 2026-10-21

- Dark minimize, maximize, restore and close icons for internal windows.
- Preserve buttons, actions, state transitions and light appearance.

### Manual binary release preparation - 2026-10-23

- Add release.ps1 to run the full build/tests and create releases/SoapUI-System-Theme.zip for manual upload to GitHub Releases.
- Preserve dist/soapui-system-theme-0.3.1.jar and include installation helpers, README, project license and third-party notices/texts in the ZIP.
- Check version references, embedded notices and exact ZIP contents before producing the release asset; remove an older ZIP when a new attempt starts.
- Clean compiled classes and copied resources before builds to prevent stale output entering a release.
- Document ready-to-install downloads, source/binary differences and the first v0.3.1 publication procedure; ignore generated release assets alongside build output.
- Keep runtime version and behavior at 0.3.1. No remote publication is performed by the release script.

### Source publication preparation - 2026-10-23

- Prepare the 0.3.1 source tree for a standalone GitHub repository.
- Add dependency notices and reference license texts; retain MIT for original project code.
- Clarify independent, unofficial status and AI-assisted development.
- Add source license headers, Git ignore/line-ending rules and GitHub setup instructions.
- Include project and dependency notices in locally built plugin JARs.
- Keep plugin runtime behavior and version at 0.3.1.

## 0.3.0 - 2026-10-21

- Readable dark headers in Preferences and New SOAP Project.
- Dark About panel and help icon.
- Reversible JavaFX styling for Endpoint Explorer and known SoapUI start pages.
- Omit the white About splash only in Dark while retaining version, copyright, build date and hyperlinks; restore the original About document in Light.
- Preserve the standard help icon's action and dimensions when showing its dark replacement.
- Apply a separate JavaFX user stylesheet without reloading pages or changing their HTML, input values or JavaScript actions; restore the original stylesheet in Light.

## 0.2.0 - 2026-10-21

- Dark legacy toolbars, improved table/tree rendering, icon contrast and scrollbars.
- Style the main, navigator, inspector/log and project-overview toolbars while retaining their original controls, actions and layout.
- Improve neutral icon contrast without modifying source images or recoloring status indicators.
- Keep tables dark when the host reapplies light stripes, and restore tree renderers and scrollbar styling in Light.

## 0.1.0 - 2026-10-21

- Windows app-theme detection, manual overrides and reversible Swing/editor styling.
