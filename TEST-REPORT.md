# Test report - SoapUI System Theme

Initial validation: 21 October 2026 (0.3.1). Later validation runs are recorded below.

## Environment

Windows; actual SoapUI Open Source 5.10.0 and its installed libraries; tests run with the bundled OpenJDK 17.0.12 runtime. Final build compiled with the installed JDK 27.0.1.0.2 using --release 17. No external dependency downloads or bundled third-party JARs.

## Passed checks

- 90 component and restoration assertions: Windows registry parsing and live read; four dark/light cycles; actual SoapUI XML editor syntax colours, caret and selection; original editor fonts, text and document identity; original look and feel, default values, table renderers and menu bar restored; new-at-night components return to light defaults.
- 45 legacy rendering checks: the actual JXToolBar and RoundButton; toolbar instance, actions and mouse targets retained; coloured child control still visible; original toolbar parent restored; late white table striping cannot overwrite dark rendering; selection stays visible; neutral icons brightened without altering red status pixels or the source image; original icon, tree renderer, tree colours and scrollbar UI restored.
- 168 internal-window control checks: real Plastic internal frames, three dark/light cycles, dark minimize/maximize/restore/close icon faces, no accidental brightening by the generic icon adapter, original buttons and listeners retained, maximize/restore/minimize/close actions still work, controls created at night styled, original UI defaults restored and live controls return to the standard light style.
- 35 dialog checks across three cycles: real SoapUI DescriptionPanel paints dark; gradient properties restore, including dialogs created at night; About information and hyperlink preserved; original About document restored by identity; editable HTML documents untouched; help icon restored and original help action retained.
- 20 JavaFX web checks using the real bundled Endpoint Explorer and offline starter page: dark body, table and input colours; delayed scene initialization; three dark/light cycles; original user stylesheet restored; input value and document identity unchanged; existing click callback still works; navigating to another page removes the stylesheet; unrelated hosts and OAuth/documentation pages excluded.
- Asynchronous controller integration: automatic light/dark/light switching; temporary unreadable OS preference retaining current theme; new components; manual override; saved preferences; return to Follow Windows. Zero document change events and identical document object and content.
- Real SoapUI plugin-loader integration: packaged JAR discovered by SoapUI's actual Reflections scanner, plugin classloader and PluginLoader. Plugin classes are absent from the test's application classpath. Metadata, activation and headless initialization checked.
- Real SoapUI desktop integration: starts SoapUI 5.10.0 with the packaged plugin, creates a synthetic local project, test suite and test case, and opens the project overview. Checks the Theme menu, dark switching, restoration, and pixel coverage of SoapUI's toolbar backgrounds. Opens actual Preferences, New SOAP Project and About dialogs. Verifies dark dialog headers through window/component events and About's dark HTML and restored splash. Captures both themes.

Total explicit component/legacy/dialog/web/window-control assertions: 358, plus the controller, loader and full desktop integration scenarios.

The real Windows preference is read separately. OS transitions in the controller test are simulated through the reader interface; the test never changes the user's actual Windows theme or scheduler.

## Visual verification

Inspected the full SoapUI desktop and project overview in dark mode and after restoring light mode. The inspected dark view includes the main toolbar, navigator, side inspectors, log toolbar, project overview toolbar, project properties and scrollbars. Also inspected actual Preferences, New SOAP Project and About in dark/light modes, and JavaFX snapshots of Endpoint Explorer and the offline starter page in dark mode.

Version 0.3.1 additionally checks real SoapUI internal-window icons before maximization, while maximized, and after restoring the window. The ordinary and maximized desktop screenshots were visually inspected: the light square control faces have been replaced by dark faces with readable light glyphs. The original actions and native Windows window decoration are retained.

The regression tests cover preservation of the blue Endpoint Explorer control, existing action objects and mouse targets. Toolbar backgrounds are drawn through a temporary Swing JLayer while keeping the actual toolbar and its controls. Light mode removes these layers. Source application files are not patched.

The tests generate PNG previews under build/ using synthetic content and the editor/table fixture. Generated screenshots and user profiles are not included in the source repository.

## Isolation

Tests use build/test-home and build/ui-home profiles. The full desktop test uses a test-only in-memory Java Preferences factory, so Java preferences are not written to the user's registry. Test windows are positioned outside the screen, have automatic focus requests disabled and cannot receive keyboard focus. No API requests or test scripts are executed.

The web test loads bundled resources with page scripts initially disabled, then executes only bundled UI scripts, skipping Google Analytics. It executes synthetic document/input/click checks and never clicks Send or Save Request. JavaFX's HTTP/2 loader attempts to create a loopback socket even for local resources; this test sandbox denies it. Only the web test sets com.sun.webkit.useHTTP2Loader=false to allow local JAR resource loading. The plugin does not modify this or any network setting. A JavaFX unnamed-module warning appears with SoapUI's classpath-based library arrangement; the web tests pass.

## Limits

Only SoapUI Open Source 5.10.0 is validated. These checks do not establish that every dialog, chart or third-party plugin is covered. The native Windows title bar, other embedded web pages, images and custom graph drawing can retain their own appearance. Semantic icon colours are retained where possible. A brief light flash during initial startup/page loading remains possible.

The official online starter URL is included in the stylesheet allowlist, but the live online page could not be downloaded/rendered in this environment. Its full visual appearance is unverified. The actual bundled offline starter page and Endpoint Explorer were rendered and checked.

Recommended acceptance check: open a normal SOAP or REST request, keep unsaved text and a selection, let the existing Windows scheduler change app mode, then open another request. Return Windows to light and check the original appearance, text and selection. Allow approximately 3-5 seconds after an OS app-mode change.

## Reproduce

Run build.ps1 with a SoapUI 5.10.0 installation and JDK 17 or newer. This compiles, packages and runs all tests, including test-ui.ps1. A Windows desktop session is required because SoapUI's JGoodies library reads screen DPI even for offscreen rendering.

## Source publication validation - 23 October 2026

The source-only GitHub package was copied to a separate clean staging directory, built with its own updated build.ps1, and tested using the installed SoapUI 5.10.0 libraries and bundled Java 17.0.12 runtime. All 358 explicit checks and the controller, real plugin-loader and real desktop scenarios passed again. The packaged plugin JAR was checked for project/third-party notices under META-INF and contains no foreign implementation classes.

All 20 Java files were compared with the previous 0.3.1 release: only MIT/copyright headers were added. Plugin runtime version and behavior remain 0.3.1. Build packaging adds license metadata; the installer error now explains that source users must build first.

Git ignore rules were exercised in an isolated test repository. Generated output, local profiles, logs and environment files are ignored, while source, documentation and license files remain trackable. Local Markdown links were checked. The source ZIP excludes generated output, all JARs, Git history and screenshots. This is a publication/packaging update, not a new runtime feature release.

## Manual binary release validation - 23 October 2026

Validated locally in the Git checkout with Windows PowerShell 5.1, JDK 27.0.1.0.2 targeting Java 17, and the actual SoapUI 5.10.0 installation (its directory is still named `C:\Program Files\SmartBear\SoapUI-5.9.1`). Tests used SoapUI's bundled Java 17.0.12 runtime.

- Ran `release.ps1 -Version 0.3.1 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.9.1' -JdkHome 'C:\Program Files\Java\jdk-27.0.1.0.2'`. All 358 explicit assertions and the controller, real plugin-loader and real desktop scenarios passed. Tests were not skipped.
- Seeded obsolete files in build/classes, build/test-classes and build/resources before the build. The clean build removed them, and they were absent from the release.
- Verified the current project license, third-party notice and all seven license reference texts inside META-INF in the JAR by SHA-256, and rejected implementation classes outside the project's package.
- Created `releases/SoapUI-System-Theme.zip` with exactly 13 files: the versioned JAR under dist/, five top-level scripts/documents/notices, and seven license texts. The release script checked paths and byte hashes against the local files. No host libraries or test profiles are included.
- Extracted the actual ZIP, ran Install.ps1 against a temporary plugin directory, and verified that the installed JAR matched the built JAR. Uninstall.ps1 removed it while retaining an unrelated plugin. Installing alongside an older System Theme JAR was blocked.
- Ran tests/release.Tests.ps1 in both Windows PowerShell 5.1 and PowerShell 7: 24 packaging/installer checks passed in each. These use a synthetic build fixture, covering repeat packaging, installer round trip, mismatched versions, build failure, missing notices, foreign classes and missing release files. They are separate from the real Java/UI tests above.
- Visually inspected newly generated actual desktop screenshots in dark mode and after restoring light mode. Theme controls, toolbars and project overview remain readable and restore correctly.
- Verified Git ignores the build log, versioned JAR and release ZIP. The user's newly uploaded .gitignore/.gitattributes were fetched; only /releases/ was added to .gitignore.

The JavaFX unnamed-module warning and SoapUI's missing optional ext-directory warning did not fail the tests. The temporary unreadable Windows preference warning is part of the controller failure-path test. The live online starter page and later SoapUI versions were not validated in this run. No changes were committed or pushed, and no GitHub release or tag was created. Publication and downloading the uploaded asset remain manual follow-up steps.

## Windows native title bars / 0.3.2 - 23 October 2026

Environment: Windows 11 build 26100, JDK 27.0.1.0.2 compiling with --release 17, SoapUI Open Source 5.10.0 and its bundled Java 17.0.12 runtime. Only temporary test profiles were used; the installed user plugin, Windows app theme and registry preferences were not modified.

- The full release.ps1 build passed the existing 358 assertions, controller scenarios, real plugin-loader integration and real SoapUI desktop integration.
- WindowsTitleBarTests passed 20 additional checks: actual DWM attribute read-back, three dark/light cycles, exact original-setting restoration, a decorated dialog opened in dark mode, preserved document text and native decorations, helper restoration on pipe closure and controller-helper shutdown. Total explicit component/native checks: 378.
- Actual compositor screenshots verified the native title bar, rather than Swing's printAll output. The standalone fixture and the packaged plugin in the actual SoapUI desktop both showed a dark native caption and restored the original light caption. The main native caption also remained dark after opening/closing the actual Preferences, New SOAP Project and About dialogs. These screenshots were visually inspected.
- The real-desktop test initially exposed interference from its own offscreen-window positioning; capture now temporarily shows only test windows without requesting keyboard focus. A disappearing dialog also exposed a per-window DWM failure. Such windows are skipped without disabling styling of other windows, and the final full-desktop run passed without a native-helper warning.
- A deliberately refusing helper was substituted only on a temporary test classpath. The asynchronous controller scenarios still passed, while logging the expected native-styling warning. This validates that helper startup failure does not undo the working Swing theme.
- The isolated 24 packaging/installer checks passed for 0.3.2. The final ZIP keeps dist/soapui-system-theme-0.3.2.jar and the existing notices/installers; its JAR includes the original helper resource and no third-party implementation libraries.

The helper only requests DWMWA_USE_IMMERSIVE_DARK_MODE for decorated AWT windows belonging to the current Java PID. It captures/restores that setting instead of forcing caption/text colors, whose original values could not be read reliably on the checked Windows version. It runs outside Swing's event thread, starts without a PowerShell profile or execution-policy change, and exits after returning to Light or closing the application.

Windows 10 takes the helper's unsupported-build path; its visual appearance was not separately tested. A blocked PowerShell/.NET environment retains ordinary native title bars. Accent/high-contrast customizations, other Java/SoapUI versions and the live online starter page are not established by this validation. No commit, push, tag or GitHub release was made for 0.3.2.

## Additional readability fixes / 0.3.2 - 23 October 2026

Scope: error-message readability and the white tab-layout icon, retained within the unpublished 0.3.2 changes. Exception handling, project-import behavior and the existing menu code were not changed in this readability update.

- ReadabilityTests passed 57 checks across three dark/light cycles: light pixels in the rendered HTML error text on a dark background, preserved document identity and text, repeat styling without clearing a copy selection, and exact restoration of unset/explicit-false HTML display properties. Editable HTML documents retain their document and display property.
- The tests use SoapUI's actual cached toggle_tabs.gif and its createToolbarButton factory. Both normal and disabled dark glyphs have no white face; dimensions, source-image pixels, Action, ButtonModel and single-click behavior are preserved. Light restores the original icons by identity.
- FullUiTests opens SoapUI's actual extended Error dialog using a synthetic error message. The packaged plugin styles its read-only HTML and restores the original display property without replacing or changing the error document. Dark/light screenshots were inspected visually.
- The full release build passes the prior 378 explicit component/native checks plus these 57 checks, for 435 in total, alongside the controller, real loader and real desktop integration scenarios. The 24 isolated packaging/installer checks remain separate from those Java/UI checks.

The display-property fix applies to default HTML text colors; it does not override explicitly styled HTML colors. Screenshots and test profiles remain under ignored build/. The final ZIP retains the 0.3.2 JAR name and complete license notices. No commit, push or release publication was performed.

## Source namespace cleanup - 25 October 2026

Java implementation classes, tests and the embedded Windows helper now live under soapui.systemtheme / soapui/systemtheme. Build/test entry points, the isolated Java Preferences factory, release validation paths and synthetic packaging fixtures use the same namespace. The published plugin group ID and version 0.3.2 remain unchanged.

The Windows/SoapUI 5.10.0 full build passed all 435 explicit checks plus the controller, actual plugin-loader and desktop integration scenarios. The loader test also checks that the published ID is retained and that the project-namespace entry point is loaded through the packaged JAR classloader. The 24 isolated packaging/installer checks passed against the renamed paths.

Compared with the previous source revision, production Java changes consist only of the package declarations and an explanatory compatibility comment. The embedded helper content is identical. The clean JAR contains the new class/resource paths with no old namespace entries. Newly generated dark/light desktop screenshots were visually checked. Copyright notices, GitHub URLs, settings and functionality are unchanged.

Only build.ps1 was used for the real build; release.ps1 was exercised solely in the temporary synthetic packaging fixture. No version bump, real release ZIP regeneration, installation into the user's plugin directory, commit, push, tag or GitHub release was performed. The existing releases/SoapUI-System-Theme.zip was checked by SHA-256 and remains unchanged.


## Reliability validation - 25 October 2026 (0.3.2 source)

This records local source validation; it does not announce a new release or update the existing published asset.
The environment remains Windows 11 build 26100, SoapUI Open Source 5.10.0 with bundled OpenJDK 17.0.12,
compiled using the installed JDK 27.0.1.0.2 with `--release 17`. The installation directory retains the name
`C:\Program Files\SmartBear\SoapUI-5.9.1` but contains `bin/soapui-5.10.0.jar`.

### Findings and changes checked

- Confirmed against the installed host bytecode that SoapUI retains its original menu bar and `MenuBuilderHelper.getMenu()` searches that bar. The desktop test temporarily reproduces the old menu-transfer algorithm and calls the actual host menu-update method. It produces a `NullPointerException`; the original menus are immediately restored in `finally`. This demonstrates the plugin-induced failure path, without claiming every project-import exception has that cause.
- The replacement uses a paint-only surface, preserving the original bar, menu objects, File-menu accelerators and mouse targets. With Dark active, the real desktop imports a synthetic saved project, selects projects/suites/cases, rebuilds dynamic menus and invokes Open Editor where available. Editors are opened/closed repeatedly. The real Groovy editor retains its document, unsaved synthetic text and selection through three additional Light/Dark cycles; no script is executed.
- Deliberately failing Swing property restoration confirms that other components and the original look-and-feel still restore. The failed component's original snapshot survives and a subsequent Light request succeeds. A separate injected failure while applying Dark also rolls back to the original properties.
- Isolated native-session tests cover startup failure, failed exchange cleanup, explicit retry with a fresh session, unsupported environments, suppressed window-event retries and application closure during helper startup. The real native tests still exercise DWM read-back, three cycles, new dialogs, EOF restoration and helper shutdown.
- Controller tests confirm no queries in saved manual mode, a fresh background query upon selecting Follow Windows, rejection of an in-flight result after selecting manual Light, restored polling after manual Dark, and no continued queries after closure.
- Plugin-loader checks compare the actual annotation and embedded binary version with `SystemThemePlugin.VERSION`. Standalone packaging tests install and uninstall from an extracted ZIP that contains no source checkout.

### Executed checks

- Full `build.ps1` with tests enabled: passed, including real JavaFX pages, real packaged-plugin discovery and the expanded SoapUI desktop scenario.
- 435 existing explicit component/legacy/control/dialog/readability/web/native checks plus 19 new failure/retry checks: **454 explicit checks**, alongside the expanded asynchronous controller, loader and desktop scenarios.
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\release.Tests.ps1`: **48 packaging/installer checks passed**, using a temporary synthetic build fixture. Guards cover the original ZIP paths/content and notices, failed builds, foreign classes, requested/script/document/annotation/notice version drift, and missing/incorrect embedded binary versions. This does not substitute for the actual full Java build.
- `git diff --check`: passed.
- Inspected the generated full-desktop Dark and restored Light views, including the original menubar and project overview. Native title-bar verification uses compositor captures rather than the simulated frame drawn by Swing `printAll`.

The new test fixtures intentionally produce warning messages for simulated native failures and unreadable OS preferences.
An intermediate recovery fixture initially targeted a color that the test label did not receive; its fault trigger was corrected.
An intermediate desktop screenshot was captured in the same EDT callback that created new components, before the controller's queued styling batch; the capture now follows that batch in a separate callback. Visual review also caught a compositor capture that showed the foreground editor instead of the test window. Native captures now wait for both SoapUI's native icon and the expected menu surface before accepting a caption pixel; unrelated foreground content is not accepted as evidence. One subsequent run was correctly rejected because the expected frame was absent from the capture; a separate desktop rerun passed. These compositor checks require an unobscured interactive test window. The final complete run passes with the functional and visual assertions enabled.

### Evidence and remaining limits

Generated evidence is under ignored `build/`: `reliability-build.log`, `reliability-packaging.log`,
`full-ui/soapui-dark.png`, `full-ui/soapui-restored.png`, `full-ui/soapui-native-dark.png`,
`full-ui/soapui-native-after-dialogs-dark.png`, `full-ui/soapui-native-restored.png` and the existing component/web/native test images.
The real desktop uses `build/ui-home`; component/controller tests use `build/test-home` or their explicit test directory.
No real user project, request, credential, theme preference or installation profile is used or modified.

Windows 10, later SoapUI versions, ReadyAPI and arbitrary third-party plugins were not newly validated.
The synthetic import does not prove compatibility with every external project, WSDL or authentication flow.
A forcibly terminated native helper cannot be guaranteed to execute its own restoration block; arbitrary OS-level process termination was not tested.
No real `release.ps1` preparation, new distribution ZIP, commit/push/tag or remote publication was performed during this task.


## Menu hover repaint regression - 25 October 2026 (0.3.2 source)

The user reported pale/white menu backgrounds after hovering over File/Project/Suite/Case in Dark.
A full-window snapshot had not exposed this: SoapUI's hardcoded white menu paint could run during an incremental
repaint without repainting the sibling dark overlay. The original menu bar and all menu objects were still present;
the defect was in paint routing, rather than menu lookup.

`MenuHoverTests` reproduces the old behavior in an isolated visible frame using the same hardcoded-white menu paint.
It dispatches mouse-enter/exit events and flushes queued dirty regions, then explicitly repaints the original bar.
Before the fix, the first File hover/partial repaint produced a **97.7% bright menu area** and failed the regression.
With the original bar inside a `JLayer` painting origin, the same partial repaints remain dark. A small synthetic
client-color marker verifies that screen captures show the test frame before their menu pixels are evaluated.

The new **46 checks** cover three Light/Dark cycles, enabled and disabled menu hover, partial repaint, an open/selected
File menu, action execution, accelerator and menu identity, resize, and restoration of the original root layout and
menu parent. Captures are taken from the compositor rather than forcing a complete Swing redraw for each hover.
The original SoapUI menu reference remains intact; no menu objects or user actions are replaced.

Validation for this change:

- Full `build.ps1` with all tests enabled: passed, including **500 explicit checks** (454 existing plus 46 hover checks),
  asynchronous controller tests, real plugin loading and the actual SoapUI desktop/import/menu/editor scenarios.
- Packaging/installer regression fixture: **48 checks passed**.
- Visually inspected the before-fix white hover bar and the fixed dark selected menu/dropdown, plus actual SoapUI Dark/Light captures.
- `git diff --check`: passed. Version remains **0.3.2**.

Generated evidence: `build/menu-before-fix.log`, `build/menu-before-fix/menu-hover-dark.png`, `build/menu-hover.log`,
`build/menu-hover/`, `build/menu-hover-build.log`, `build/menu-hover-packaging.log` and `build/full-ui/`.
The interactive test window must remain unobscured. Existing Windows/SoapUI compatibility limits still apply.
No installation into the normal user profile, release preparation, commit, tag, push or publication was performed.
