# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

<#
.SYNOPSIS
Build, run all tests, and prepare the binary ZIP for a manual GitHub Release.
.DESCRIPTION
Requires the same Windows desktop, SoapUI and JDK as build.ps1. There is no
SkipTests option. Writes releases/SoapUI-System-Theme.zip only after validation.
Does not install the plugin, commit, tag, push, or publish anything.
#>
[CmdletBinding()]
param(
    [string]$SoapUiHome,
    [string]$JdkHome,
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$root = $PSScriptRoot
$releaseDirectory = Join-Path $root 'releases'
$output = Join-Path $releaseDirectory 'SoapUI-System-Theme.zip'
# A failed attempt must not leave an older ZIP looking ready to upload.
if (Test-Path -LiteralPath $output -PathType Leaf) { Remove-Item -LiteralPath $output }

$pluginSource = Get-Content -LiteralPath (Join-Path $root 'src/main/java/soapui/systemtheme/SystemThemePlugin.java') -Raw
$versionMatch = [regex]::Match($pluginSource, '\bVERSION\s*=\s*"(\d+\.\d+\.\d+)"')
if (-not $versionMatch.Success) { throw 'Cannot read the version from SystemThemePlugin.java.' }
if ($pluginSource -notmatch '\bversion\s*=\s*SystemThemePlugin\.VERSION') { throw 'Plugin annotation must use the central VERSION.' }
$pluginVersion = $versionMatch.Groups[1].Value
if ($Version -and $Version -cne $pluginVersion) { throw "Requested version $Version differs from plugin version $pluginVersion." }
$Version = $pluginVersion
$jarName = "soapui-system-theme-$Version.jar"
$noticeSource = Get-Content -LiteralPath (Join-Path $root 'THIRD_PARTY_NOTICES.md') -Raw
$noticeVersions = @([regex]::Matches($noticeSource, 'SoapUI System Theme (\d+\.\d+\.\d+)\b'))
if ($noticeVersions.Count -ne 1 -or $noticeVersions[0].Groups[1].Value -cne $Version) { throw 'Version mismatch in THIRD_PARTY_NOTICES.md.' }
foreach ($script in 'build.ps1','Install.ps1','Uninstall.ps1','test-ui.ps1') {
    $content = Get-Content -LiteralPath (Join-Path $root $script) -Raw
    $names = @([regex]::Matches($content, 'soapui-system-theme-\d+\.\d+\.\d+\.jar') | ForEach-Object { $_.Value })
    if ($content -notmatch 'soapui-system-theme-\$version\.jar') { throw "$script does not reference the central versioned JAR." }
    $versionSource = if ($script -in @('Install.ps1','Uninstall.ps1')) { 'THIRD_PARTY_NOTICES.md' } else { 'SystemThemePlugin.java' }
    if (-not $content.Contains($versionSource)) { throw "$script does not read its required version source." }
    # test-ui.ps1 also removes known older versions from its isolated test profile.
    if ($script -ne 'test-ui.ps1' -and @($names | Where-Object { $_ -cne $jarName }).Count -gt 0) {
        throw "Version mismatch in $script. Expected $jarName."
    }
}
$versionChecks = @{
    'README.md' = "(?m)^# SoapUI System Theme $([regex]::Escape($Version))\r?$"
    'CHANGELOG.md' = "(?m)^## $([regex]::Escape($Version)) - "
    'THIRD_PARTY_NOTICES.md' = "SoapUI System Theme $([regex]::Escape($Version))\b"
    'src/main/java/soapui/systemtheme/ThemeController.java' = 'System Theme " \+ SystemThemePlugin\.VERSION \+ " started'
}
foreach ($file in $versionChecks.Keys) {
    if ((Get-Content -LiteralPath (Join-Path $root $file) -Raw) -notmatch $versionChecks[$file]) {
        throw "Version mismatch in $file. Expected $Version."
    }
}
$documents = @('Install.ps1','Uninstall.ps1','README.md','LICENSE','THIRD_PARTY_NOTICES.md')
foreach ($file in $documents) {
    if (-not (Test-Path -LiteralPath (Join-Path $root $file) -PathType Leaf)) { throw "Missing release file: $file" }
}
$licenseDirectory = Join-Path $root 'licenses'
$licenseFiles = @(Get-ChildItem -LiteralPath $licenseDirectory -File -Recurse)
if ($licenseFiles.Count -eq 0) { throw 'licenses/ must contain the third-party license texts.' }

Write-Host "Building and testing System Theme $Version for release..."
$buildParameters = @{}
if ($SoapUiHome) { $buildParameters.SoapUiHome = $SoapUiHome }
if ($JdkHome) { $buildParameters.JdkHome = $JdkHome }
& (Join-Path $root 'build.ps1') @buildParameters
if (-not $?) { throw 'Full build failed; no release ZIP was created.' }
$jarPath = Join-Path $root "dist/$jarName"
if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) { throw "Build did not produce $jarPath." }

# Verify the JAR contains project classes and the current notices, without host libraries.
$jarArchive = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $binaryVersion = $jarArchive.GetEntry('META-INF/soapui-system-theme.version')
    if (-not $binaryVersion) { throw 'JAR is missing its embedded version.' }
    $versionReader = New-Object IO.StreamReader($binaryVersion.Open())
    try { $builtVersion = $versionReader.ReadToEnd().Trim() } finally { $versionReader.Dispose() }
    if ($builtVersion -cne $Version) { throw 'JAR version differs from the central VERSION.' }
    if (-not $jarArchive.GetEntry('soapui/systemtheme/SystemThemePlugin.class')) { throw 'Plugin entry point is missing from the JAR.' }
    foreach ($entry in $jarArchive.Entries) {
        if ($entry.FullName.EndsWith('.class') -and -not $entry.FullName.StartsWith('soapui/systemtheme/')) {
            throw "Unexpected implementation class in JAR: $($entry.FullName)"
        }
    }
    $noticeFiles = @('LICENSE','THIRD_PARTY_NOTICES.md') + @($licenseFiles | ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\','/') })
    foreach ($file in $noticeFiles) {
        $entry = $jarArchive.GetEntry("META-INF/$file")
        if (-not $entry) { throw "JAR is missing META-INF/$file." }
        $stream = $entry.Open()
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $hash = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','') }
        finally { $stream.Dispose(); $sha.Dispose() }
        if ($hash -ne (Get-FileHash -LiteralPath (Join-Path $root $file) -Algorithm SHA256).Hash) {
            throw "JAR contains an outdated notice: $file"
        }
    }
} finally { $jarArchive.Dispose() }

$stage = Join-Path $root ("build/release-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage -Force | Out-Null
try {
    $payload = Join-Path $stage 'payload'
    New-Item -ItemType Directory -Path (Join-Path $payload 'dist') -Force | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $payload "dist/$jarName")
    foreach ($file in $documents) { Copy-Item -LiteralPath (Join-Path $root $file) -Destination $payload }
    Copy-Item -LiteralPath $licenseDirectory -Destination $payload -Recurse
    $temporaryZip = Join-Path $stage 'SoapUI-System-Theme.zip'
    # Windows PowerShell 5.1's CreateFromDirectory can write backslash names.
    # Explicit forward slashes keep the ZIP portable and preserve dist/ reliably.
    $zip = [IO.Compression.ZipFile]::Open($temporaryZip, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($file in (Get-ChildItem -LiteralPath $payload -File -Recurse)) {
            $name = $file.FullName.Substring($payload.Length + 1).Replace('\','/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $name) | Out-Null
        }
    } finally { $zip.Dispose() }
    $expected = @("dist/$jarName") + $documents + @($licenseFiles | ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\','/') })
    $zip = [IO.Compression.ZipFile]::OpenRead($temporaryZip)
    try {
        $actual = @($zip.Entries | ForEach-Object { $_.FullName.Replace('\','/') })
        if (Compare-Object ($expected | Sort-Object) ($actual | Sort-Object) -CaseSensitive) { throw 'Unexpected release ZIP contents.' }
        foreach ($entry in $zip.Entries) {
            $stream = $entry.Open()
            $sha = [Security.Cryptography.SHA256]::Create()
            try { $hash = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','') }
            finally { $stream.Dispose(); $sha.Dispose() }
            if ($hash -ne (Get-FileHash -LiteralPath (Join-Path $root $entry.FullName) -Algorithm SHA256).Hash) {
                throw "ZIP content differs from the tested source: $($entry.FullName)"
            }
        }
    } finally { $zip.Dispose() }
    New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
    Move-Item -LiteralPath $temporaryZip -Destination $output
} finally {
    # Delete only this script's uniquely named staging folder inside this checkout.
    $stageFull = [IO.Path]::GetFullPath($stage)
    $buildFull = [IO.Path]::GetFullPath((Join-Path $root 'build')) + [IO.Path]::DirectorySeparatorChar
    if (-not $stageFull.StartsWith($buildFull, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe staging cleanup path.' }
    Remove-Item -LiteralPath $stageFull -Recurse -Force
}
Write-Host "Release $Version ready: $output"
Write-Host "SHA256: $((Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash)"
Write-Host 'Upload this ZIP manually to the matching GitHub Release. Nothing has been published.'
