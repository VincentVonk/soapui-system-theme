# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

# Isolated packaging/installer checks, with a synthetic build in a temporary copy.
# This does not validate Java code or replace release.ps1's real full build/tests.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$root = Split-Path -Parent $PSScriptRoot
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('soapui-release-tests-' + [guid]::NewGuid().ToString('N'))
$checks = 0
function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}
function Assert-Failure([scriptblock]$Action, [string]$MessagePattern) {
    $caught = $null
    try { & $Action } catch { $caught = $_ }
    Assert-True ($null -ne $caught) 'Expected release failure.'
    Assert-True ($caught.Exception.Message -match $MessagePattern) "Unexpected failure: $caught"
    Assert-True (-not (Test-Path -LiteralPath $zipPath)) 'A failed release left a ZIP behind.'
}
New-Item -ItemType Directory -Path $fixture | Out-Null
try {
    foreach ($file in 'release.ps1','Install.ps1','Uninstall.ps1','test-ui.ps1','README.md','CHANGELOG.md','LICENSE','THIRD_PARTY_NOTICES.md') {
        Copy-Item -LiteralPath (Join-Path $root $file) -Destination $fixture
    }
    Copy-Item -LiteralPath (Join-Path $root 'licenses') -Destination $fixture -Recurse
    $sourceDirectory = Join-Path $fixture 'src/main/java/nl/vincentvonk/soapui/theme'
    New-Item -ItemType Directory -Path $sourceDirectory -Force | Out-Null
    foreach ($file in 'SystemThemePlugin.java','ThemeController.java') {
        Copy-Item -LiteralPath (Join-Path $root "src/main/java/nl/vincentvonk/soapui/theme/$file") -Destination $sourceDirectory
    }
    $pluginSource = Get-Content -LiteralPath (Join-Path $sourceDirectory 'SystemThemePlugin.java') -Raw
    $version = [regex]::Match($pluginSource, '\bversion\s*=\s*"(\d+\.\d+\.\d+)"').Groups[1].Value
    $jarName = "soapui-system-theme-$version.jar"
    $fakeBuild = @'
param([string]$SoapUiHome, [string]$JdkHome)
$ErrorActionPreference = 'Stop'
if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'fail-build')) { throw 'Synthetic build failure.' }
$payload = Join-Path $PSScriptRoot 'build/fake-jar'
New-Item -ItemType Directory -Path (Join-Path $payload 'nl/vincentvonk/soapui/theme'),(Join-Path $payload 'META-INF'),(Join-Path $PSScriptRoot 'dist') -Force | Out-Null
Set-Content -LiteralPath (Join-Path $payload 'nl/vincentvonk/soapui/theme/SystemThemePlugin.class') -Value 'Synthetic test entry point; not executable Java.'
foreach ($file in 'LICENSE','THIRD_PARTY_NOTICES.md') { Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination (Join-Path $payload 'META-INF') -Force }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'licenses') -Destination (Join-Path $payload 'META-INF') -Recurse -Force
if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'omit-notice')) { Remove-Item -LiteralPath (Join-Path $payload 'META-INF/LICENSE') }
if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'foreign-class')) { Set-Content -LiteralPath (Join-Path $payload 'Foreign.class') -Value 'Must not ship' }
$target = Join-Path $PSScriptRoot 'dist/__JAR__'
if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target }
$archive = [IO.Compression.ZipFile]::Open($target, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in (Get-ChildItem -LiteralPath $payload -File -Recurse)) {
        $name = $file.FullName.Substring($payload.Length + 1).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $name) | Out-Null
    }
} finally { $archive.Dispose() }
'@
    Set-Content -LiteralPath (Join-Path $fixture 'build.ps1') -Value $fakeBuild.Replace('__JAR__',$jarName)
    $releaseScript = Join-Path $fixture 'release.ps1'
    $zipPath = Join-Path $fixture 'releases/SoapUI-System-Theme.zip'
    & $releaseScript -Version $version
    Assert-True (Test-Path -LiteralPath $zipPath -PathType Leaf) 'Release ZIP was not created.'
    $archive = [IO.Compression.ZipFile]::OpenRead($zipPath)
    try {
        $expected = @("dist/$jarName",'Install.ps1','Uninstall.ps1','README.md','LICENSE','THIRD_PARTY_NOTICES.md') + @(
            Get-ChildItem -LiteralPath (Join-Path $fixture 'licenses') -File -Recurse | ForEach-Object { $_.FullName.Substring($fixture.Length + 1).Replace('\','/') }
        )
        $actual = @($archive.Entries | ForEach-Object { $_.FullName.Replace('\','/') })
        Assert-True ($null -eq (Compare-Object $expected $actual -CaseSensitive)) 'ZIP paths do not match the binary distribution contract.'
    } finally { $archive.Dispose() }
    $extracted = Join-Path $fixture 'extracted'
    [IO.Compression.ZipFile]::ExtractToDirectory($zipPath, $extracted)
    $plugins = Join-Path $fixture 'temporary-plugins'
    & (Join-Path $extracted 'Install.ps1') -PluginDirectory $plugins
    $installed = Join-Path $plugins $jarName
    Assert-True ((Get-FileHash -LiteralPath $installed).Hash -eq (Get-FileHash -LiteralPath (Join-Path $fixture "dist/$jarName")).Hash) 'Installer changed the JAR.'
    Set-Content -LiteralPath (Join-Path $plugins 'keep.txt') -Value 'Unrelated file'
    & (Join-Path $extracted 'Uninstall.ps1') -PluginDirectory $plugins
    Assert-True (-not (Test-Path -LiteralPath $installed)) 'Uninstall did not remove the plugin.'
    Assert-True (Test-Path -LiteralPath (Join-Path $plugins 'keep.txt')) 'Uninstall removed an unrelated file.'
    & $releaseScript -Version $version
    Assert-True (Test-Path -LiteralPath $zipPath) 'Repeated release failed.'
    Assert-Failure { & $releaseScript -Version '999.0.0' } 'Requested version'

    $installPath = Join-Path $fixture 'Install.ps1'
    $installSource = Get-Content -LiteralPath $installPath -Raw
    Set-Content -LiteralPath $installPath -Value $installSource.Replace($jarName,'soapui-system-theme-999.0.0.jar')
    Assert-Failure { & $releaseScript } 'Install.ps1 does not reference'
    Set-Content -LiteralPath $installPath -Value $installSource

    foreach ($case in @(
        @{ Marker = 'fail-build'; Error = 'Synthetic build failure' },
        @{ Marker = 'omit-notice'; Error = 'JAR is missing META-INF/LICENSE' },
        @{ Marker = 'foreign-class'; Error = 'Unexpected implementation class' }
    )) {
        $marker = Join-Path $fixture $case.Marker
        Set-Content -LiteralPath $marker -Value 'Test failure guard'
        Assert-Failure { & $releaseScript } $case.Error
        Remove-Item -LiteralPath $marker
    }
    $savedLicense = Get-Content -LiteralPath (Join-Path $fixture 'LICENSE') -Raw
    Remove-Item -LiteralPath (Join-Path $fixture 'LICENSE')
    Assert-Failure { & $releaseScript } 'Missing release file: LICENSE'
    Set-Content -LiteralPath (Join-Path $fixture 'LICENSE') -Value $savedLicense
    Write-Host "PASS: $checks packaging/installer checks (synthetic build fixture)."
} finally {
    $fullPath = [IO.Path]::GetFullPath($fixture)
    $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if (-not $fullPath.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -or (Split-Path -Leaf $fullPath) -notlike 'soapui-release-tests-*') { throw 'Unsafe test cleanup path.' }
    Remove-Item -LiteralPath $fullPath -Recurse -Force
}
