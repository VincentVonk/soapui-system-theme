# GitHub setup and ongoing development

This guide covers initial source-repository setup and ongoing development for version **0.3.2**.
For the ready-to-install binary ZIP, use the [README installation guide](../README.md#installation).
Source archives contain tests, build scripts and notices; their files belong directly in the repository root.

## Initial upload

1. Extract the **source archive** into an empty directory. The binary release ZIP is a separate installation package.
2. Create your GitHub repository, for example `soapui-system-theme`. This package already includes a README, MIT license, and .gitignore; do not ask GitHub to generate them again.
3. Upload the **contents** to the root of the repository. Include .gitignore, .gitattributes, docs/, and licenses/. Do not upload the ZIP itself as a substitute for the extracted source files.
4. Check that GitHub shows README.md, LICENSE, THIRD_PARTY_NOTICES.md, build.ps1, and src/ at the top level.

## Local clone

Clone the repository into your usual development directory:

~~~powershell
git clone <your-repository-url>
~~~

When sharing a development environment, identify the GitHub URL and the absolute path of the local clone so collaborators can use the intended checkout. Private repositories also require access through the account or local environment being used.

Build output, JARs, ZIPs, local SoapUI profiles, and environment files are listed in .gitignore.
A source archive has no .git directory; cloning the repository obtains its Git history.

## Building and installing

Follow [DEVELOPMENT.md](DEVELOPMENT.md#building-from-source) for prerequisites, building, `-SoapUiHome`/`-JdkHome` options and tests.
Use the [README](../README.md#installation) to install a built JAR or the binary release. Close SoapUI first and keep only one plugin version installed.

## Subsequent updates

Work from the clone, review changes with `git diff`, and commit the intended source files and documentation.
Follow [RELEASING.md](RELEASING.md) for the full manual build/test/package, source-tag and publication procedure.
The release script prepares the ZIP locally; you handle Git operations and GitHub publication yourself.
