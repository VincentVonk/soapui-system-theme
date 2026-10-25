# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

param([Parameter(Mandatory=$true)][string]$SoapUiHome)
$ErrorActionPreference = 'Stop'
$pluginSource = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'src/main/java/soapui/systemtheme/SystemThemePlugin.java') -Raw
$versionMatch = [regex]::Match($pluginSource, '\bVERSION\s*=\s*"(\d+\.\d+\.\d+)"')
if (-not $versionMatch.Success) { throw 'Cannot read the central plugin VERSION.' }
$version = $versionMatch.Groups[1].Value
$base = $PSScriptRoot
$uiHome = Join-Path $base 'build\ui-home'
$plugins = Join-Path $uiHome '.soapuios\plugins'
New-Item -ItemType Directory -Path $plugins -Force | Out-Null
$oldTestJar = Join-Path $plugins 'soapui-system-theme-0.1.0.jar'
if (Test-Path -LiteralPath $oldTestJar) { Remove-Item -LiteralPath $oldTestJar }
$previousTestJar = Join-Path $plugins 'soapui-system-theme-0.2.0.jar'
if (Test-Path -LiteralPath $previousTestJar) { Remove-Item -LiteralPath $previousTestJar }
$previousPatchJar = Join-Path $plugins 'soapui-system-theme-0.3.0.jar'
if (Test-Path -LiteralPath $previousPatchJar) { Remove-Item -LiteralPath $previousPatchJar }
$previousReleaseJar = Join-Path $plugins 'soapui-system-theme-0.3.1.jar'
if (Test-Path -LiteralPath $previousReleaseJar) { Remove-Item -LiteralPath $previousReleaseJar }
Copy-Item -LiteralPath (Join-Path $base "dist\soapui-system-theme-$version.jar") -Destination (Join-Path $plugins "soapui-system-theme-$version.jar") -Force
'mode=LIGHT' | Set-Content -LiteralPath (Join-Path $uiHome '.soapuios\system-theme.properties')
$cp = (Join-Path $SoapUiHome 'bin\soapui-5.10.0.jar') + ';' + (Join-Path $SoapUiHome 'lib\*')
$nativeOptions = @()
if (Test-Path -LiteralPath (Join-Path $base 'build\native-titlebar\supported')) { $nativeOptions = @('-Dsoapui.theme.testNativeTitleBar=true') }
& (Join-Path $SoapUiHome 'jre\bin\java.exe') '-Djava.awt.headless=false' @nativeOptions "-Duser.home=$uiHome" "-Djava.io.tmpdir=$(Join-Path $base 'build\tmp')" "-Dsoapui.home=$(Join-Path $SoapUiHome 'bin')" --add-opens=java.desktop/javax.swing=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/java.beans=ALL-UNNAMED -cp "$(Join-Path $base 'build\test-classes');$cp" soapui.systemtheme.FullUiTests (Join-Path $base 'build\full-ui')
if ($LASTEXITCODE -ne 0) { throw 'Full SoapUI desktop integration test failed.' }
