# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

param([string]$PluginDirectory = (Join-Path $env:USERPROFILE '.soapuios\plugins'))
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$fileName = 'soapui-system-theme-0.3.1.jar'
$source = Join-Path $PSScriptRoot "dist\$fileName"
if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Missing $source. Run build.ps1 first, or extract a complete binary release." }
$destination = [IO.Path]::GetFullPath($PluginDirectory)
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$otherVersions = @(Get-ChildItem -LiteralPath $destination -Filter 'soapui-system-theme-*.jar' | Where-Object { $_.Name -ne $fileName })
if ($otherVersions.Count -gt 0) { throw 'Another version of System Theme is installed. Remove that JAR before installing this version.' }
Copy-Item -LiteralPath $source -Destination (Join-Path $destination $fileName) -Force
Write-Host "Installed: $(Join-Path $destination $fileName)"
Write-Host 'Restart SoapUI Open Source 5.10.0. Theme > Follow Windows is the default.'
