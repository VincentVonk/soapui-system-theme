# Test report - SoapUI System Theme 0.3.1

Date: 21 October 2026.

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
