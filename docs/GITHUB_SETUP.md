# GitHub setup and ongoing development

This package contains the source code for plugin version **0.3.1**, including tests, build scripts, and license notices. The files belong directly in the root of your repository.

## Initial upload

1. Extract the ZIP into an empty directory.
2. Create your GitHub repository, for example `soapui-system-theme`. This package already includes a README, MIT license, and .gitignore; do not ask GitHub to generate them again.
3. Upload the **contents** to the root of the repository. Include .gitignore, .gitattributes, docs/, and licenses/. Do not upload the ZIP itself as a substitute for the extracted source files.
4. Check that GitHub shows README.md, LICENSE, THIRD_PARTY_NOTICES.md, build.ps1, and src/ at the top level.

## Local clone

Clone the repository into your usual development directory:

~~~powershell
git clone <your-repository-url>
~~~

Share the GitHub URL and the absolute path of the local clone in the chat so that future work can use the existing repository. A private repository must be accessible through your connected GitHub account or local development environment.

Build output, JARs, ZIPs, local SoapUI profiles, and environment files are listed in .gitignore. The package contains no .git directory or existing Git history.

## Building and installing

Open PowerShell in the repository root with a JDK 17 or newer and SoapUI Open Source 5.10.0 installed:

~~~powershell
.\build.ps1
~~~

Use `-SoapUiHome` and `-JdkHome` if automatic detection fails; see the README. The build creates `dist\soapui-system-theme-0.3.1.jar` and runs the tests. A Windows desktop session is required for graphical tests.

Close SoapUI before installing the JAR or running `.\Install.ps1`. Keep only one plugin version in your plugins directory.

## Subsequent updates

Work from the clone, review changes with `git diff`, and commit the intended source files and documentation. To prepare a binary release, run `.\release.ps1 -Version 0.3.1`. It always runs the full build/tests and writes `releases\SoapUI-System-Theme.zip`, retaining the versioned JAR under `dist/` inside the ZIP. Follow the [manual release procedure](../README.md#preparing-a-manual-release-maintainers) to publish that asset yourself. The script does not commit, tag, push or publish.
