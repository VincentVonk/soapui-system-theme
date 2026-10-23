# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

param([string]$PluginDirectory = (Join-Path $env:USERPROFILE '.soapuios\plugins'))
$ErrorActionPreference = 'Stop'
$target = Join-Path ([IO.Path]::GetFullPath($PluginDirectory)) 'soapui-system-theme-0.3.1.jar'
if (Test-Path -LiteralPath $target -PathType Leaf) { Remove-Item -LiteralPath $target }
Write-Host 'System Theme 0.3.1 removed. Restart SoapUI to restore its ordinary appearance.'
Write-Host 'The saved theme preference is retained in .soapuios\system-theme.properties.'
