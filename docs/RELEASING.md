# Preparing a manual release

Maintainer guide for the current **0.3.2** source tree.
See [development prerequisites and tests](DEVELOPMENT.md) before preparing an artifact.
The current process builds locally and publishes manually through GitHub Releases; the scripts do not publish automatically.

## 1. Choose the version and source revision

The checked-in version is **0.3.2**. The examples below prepare that version locally.
GitHub already has a published `v0.3.2` release: local preparation does not authorize replacing its asset or moving its tag.
For a future public release, choose a new version and update the references together in a separate source change.

`release.ps1` reads the version from `src/main/java/soapui/systemtheme/SystemThemePlugin.java`.
Its optional `-Version` parameter is an assertion, rather than a version-bump operation.
Before building, it checks:

| Checked item | Required relationship |
| --- | --- |
| Plugin annotation | A three-part numeric version, currently `0.3.2`. |
| `build.ps1`, `Install.ps1`, `Uninstall.ps1` | The matching `soapui-system-theme-0.3.2.jar` reference, without a different versioned JAR reference. |
| `test-ui.ps1` | The current JAR reference; older JAR names are also allowed for isolated-profile cleanup. |
| `README.md` | The exact versioned top-level heading shown below. |
| `CHANGELOG.md` | A matching `## 0.3.2 - ...` version entry. |
| `THIRD_PARTY_NOTICES.md` | A matching `SoapUI System Theme 0.3.2` reference. |
| `ThemeController.java` | A matching version in its startup message. |

The required README heading is:

```markdown
# SoapUI System Theme 0.3.2
```

**Documentation constraint:** the existing script checks that literal heading. An unversioned product title fails validation.
The README therefore retains the version in its H1. A future change to this rule requires a separately reviewed script change;
do not remove the heading, hide a substitute heading or weaken the check during a documentation edit.

For a future version, also review the About/startup strings in `ThemeController.java`, the message in `Uninstall.ps1`,
README installation/examples and the remaining documentation references. The automatic checks do not cover every occurrence of a version string.
Keep the Java namespace `soapui.systemtheme` and the published plugin ID stable unless a separately reviewed change requires otherwise.

## 2. Prepare and validate the binary ZIP

Run from the repository root with a working Windows desktop, SoapUI 5.10.0 and a JDK 17 or newer:

```powershell
.\release.ps1 -Version 0.3.2
```

If automatic detection fails:

```powershell
.\release.ps1 -Version 0.3.2 -SoapUiHome 'C:\Program Files\SmartBear\SoapUI-5.10.0' -JdkHome 'C:\Program Files\Java\jdk-27.0.1.0.2'
```

Use your installed paths. An older SoapUI directory name is acceptable when it contains `bin\soapui-5.10.0.jar`.

Every release preparation calls `build.ps1` with **all tests enabled**, including `test-ui.ps1` and its actual SoapUI desktop scenario.
There is no `release.ps1 -SkipTests` option. A successful compilation-only build is insufficient.
The build cleans compiled classes and copied resources before compiling.

The release script then verifies:

- The expected plugin entry point exists in the JAR and implementation classes are inside `soapui/systemtheme/`.
- `LICENSE`, `THIRD_PARTY_NOTICES.md` and every file in `licenses/` are embedded under `META-INF/` with matching SHA-256 hashes.
- The ZIP has the expected paths and file contents, with no extra distribution files.

Only after those checks does it write:

```text
releases/SoapUI-System-Theme.zip
```

The versioned JAR remains in `dist/` as well as inside the ZIP. The ZIP name stays fixed for every version.
A previous ZIP is removed at the start of a new release attempt; a failed attempt must not leave an older ZIP looking ready to upload.
The script prints a SHA-256 digest of the final ZIP. Repeated runs use the same layout and checks, but archive timestamps may differ.

The script does not install the plugin into your real profile, commit, tag, push or publish anything.

## 3. Check the distribution layout

Extract the complete archive into an empty directory. Its contents start at the ZIP root, without an extra wrapper folder:

```text
SoapUI-System-Theme.zip
├── dist/
│   └── soapui-system-theme-0.3.2.jar
├── Install.ps1
├── Uninstall.ps1
├── README.md
├── LICENSE
├── THIRD_PARTY_NOTICES.md
└── licenses/
    └── all seven reference license texts
```

The current package contains **13 files**. Keep `dist/` intact: `Install.ps1` resolves its versioned JAR relative to the extracted script.
The JAR contains the original Windows helper and the embedded notices; no SoapUI/JDK or other third-party JARs are redistributed.
The ZIP excludes Java sources, test profiles, generated screenshots, Git history and the supporting `docs/` tree.
README links to supporting documentation/screenshots use GitHub URLs so they do not require a `docs/` directory beside the extracted README.
New documentation links become available on GitHub after the matching documentation changes are pushed.

## 4. Run the packaging regression checks

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\release.Tests.ps1
```

This is the existing test invocation. It applies the execution-policy option only to that test process; follow local policy.
Where scripts are already permitted, `.\tests\release.Tests.ps1` can be run directly.
These checks copy scripts/notices into a temporary fixture and substitute a synthetic build.
They cover repeated packaging, paths, an installer/uninstaller round trip and failure guards for versions, build errors,
missing notices and unexpected implementation classes. The fixture is removed afterward.

The recorded result is 24 checks. They validate packaging behavior, not Java functionality, native Windows styling or live GitHub publication.
They do not replace the real full build/tests run by `release.ps1`.

## 5. Review the tested artifact

- Read the complete build/test output and compare it with the dated [test report](../TEST-REPORT.md); historical passing results are not a new run.
- Inspect the extracted JAR name, scripts, README and all notices.
- Review generated test screenshots under `build/`, including Dark and restored Light. Native-title-bar checks require compositor captures.
- Test installation/removal in a temporary plugin directory if needed, without changing the real profile.

For example, from the extracted binary directory:

```powershell
$testPlugins = Join-Path $env:TEMP ('soapui-theme-install-check-' + [guid]::NewGuid().ToString('N'))
.\Install.ps1 -PluginDirectory $testPlugins
.\Uninstall.ps1 -PluginDirectory $testPlugins
```

The installer refuses a different System Theme JAR alongside the intended version.
The uninstaller removes only its versioned JAR; saved theme preferences are retained.
For a real installation, close SoapUI and follow the [README installation steps](../README.md#installation).

Record the ZIP digest for comparison with the uploaded asset:

```powershell
Get-FileHash -LiteralPath .\releases\SoapUI-System-Theme.zip -Algorithm SHA256
```

## 6. Review, commit and identify the source

Review the source/docs changes with your usual Git tools, including new files:

```powershell
git status --short
git diff --check
git diff
```

Stage and commit only the reviewed source/documentation changes, then push them yourself.
Generated `build/`, `dist/` and `releases/` content is ignored and belongs outside the source commit.
Record the full source commit SHA:

```powershell
git rev-parse HEAD
```

The release must identify the same source and packaged documentation that were tested.
If any source or packaged document changes after validation, run `release.ps1` again before uploading the ZIP.

For a **new** version, either create its `v<version>` tag on that exact commit in GitHub's release form,
or create and push an annotated tag locally after committing/pushing the reviewed source.
The local sequence is shown below; set the variable to the intended version whose tag does not already exist.
The existing `v0.3.2` tag must not be recreated or moved.

```powershell
$releaseVersion = '0.3.2'
git ls-remote --tags origin "refs/tags/v$releaseVersion" "refs/tags/v$releaseVersion^{}"
```

If the remote tag exists, verify its commit rather than creating it again; an annotated tag's `^{}` line identifies the commit.
Only when it is a new version and the tag does not exist:

```powershell
git tag -a "v$releaseVersion" -m "SoapUI System Theme $releaseVersion"
git push origin "v$releaseVersion"
```

These are manual maintainer actions; neither `build.ps1` nor `release.ps1` performs them.

## 7. Publish through GitHub Releases

1. Open [GitHub Releases](https://github.com/VincentVonk/soapui-system-theme/releases) and choose **Draft a new release**.
2. Choose the matching existing tag, or create a new `v<version>` tag against the reviewed source commit. Verify the target before publication.
3. Use the title `SoapUI System Theme <version>`. Add release notes from the matching [changelog](../CHANGELOG.md), including supported Windows/SoapUI versions and relevant limitations.
4. Attach the tested **SoapUI-System-Theme.zip** in the separate binary-assets upload area. Keep that filename exact.
5. Review the draft and its attachment. Publish as a full release and mark it **Latest** when it is intended to serve the permanent latest-download link.

GitHub's automatic **Source code** downloads are separate archives and do not replace the uploaded binary ZIP.
If the intended tag/release already exists, inspect it rather than silently repointing it to different source or replacing an artifact.

## 8. Verify the published download

Check both permanent links:

- [Latest release](https://github.com/VincentVonk/soapui-system-theme/releases/latest)
- [Latest binary ZIP](https://github.com/VincentVonk/soapui-system-theme/releases/latest/download/SoapUI-System-Theme.zip)

Download the **uploaded binary asset**, extract it and verify the versioned JAR and installation layout.
Compare its SHA-256 with the locally tested ZIP using `Get-FileHash -LiteralPath` on the downloaded file.
A match verifies byte identity; it does not replace testing or review of the source and release target.

GitHub documents [manual release creation](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
and [permanent release/asset links](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases).
The permanent asset link requires a published latest release with an asset named exactly `SoapUI-System-Theme.zip`.
