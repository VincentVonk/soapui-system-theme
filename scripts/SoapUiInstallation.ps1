# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

function Resolve-SoapUiInstallation {
    param([string]$SoapUiHome)

    $supportedVersions = @('5.10.1', '5.10.0')
    if ($SoapUiHome) {
        $candidates = @(Get-Item -LiteralPath $SoapUiHome -ErrorAction SilentlyContinue |
            Where-Object { $_.PSIsContainer })
    } else {
        $candidates = @(Get-ChildItem -Path "$env:ProgramFiles\SmartBear\SoapUI*", "$env:ProgramFiles\SoapUI*" -Directory -ErrorAction SilentlyContinue)
    }

    foreach ($version in $supportedVersions) {
        foreach ($candidate in $candidates) {
            $jar = Join-Path $candidate.FullName "bin\soapui-$version.jar"
            $lib = Join-Path $candidate.FullName 'lib'
            if ((Test-Path -LiteralPath $jar -PathType Leaf) -and
                (Test-Path -LiteralPath $lib -PathType Container)) {
                return [pscustomobject]@{ Home = $candidate.FullName; Jar = $jar; Version = $version }
            }
        }
    }

    throw 'Specify -SoapUiHome pointing to SoapUI Open Source 5.10.0 or 5.10.1 (the folder containing bin and lib).'
}
