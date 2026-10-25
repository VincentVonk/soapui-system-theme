# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

param([string]$PluginDirectory = (Join-Path $env:USERPROFILE '.soapuios\plugins'))
$ErrorActionPreference = 'Stop'
# The notices ship in the existing binary ZIP; no source checkout is required.
$notices = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'THIRD_PARTY_NOTICES.md') -Raw
$versions = @([regex]::Matches($notices, 'SoapUI System Theme (\d+\.\d+\.\d+)\b'))
if ($versions.Count -ne 1) { throw 'Cannot read a unique version from THIRD_PARTY_NOTICES.md.' }
$version = $versions[0].Groups[1].Value
$target = Join-Path ([IO.Path]::GetFullPath($PluginDirectory)) "soapui-system-theme-$version.jar"
if (Test-Path -LiteralPath $target -PathType Leaf) { Remove-Item -LiteralPath $target }
Write-Host "System Theme $version removed. Restart SoapUI to restore its ordinary appearance."
Write-Host 'The saved theme preference is retained in .soapuios\system-theme.properties.'
