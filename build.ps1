# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk

param(
    [string]$SoapUiHome,
    [string]$JdkHome,
    [switch]$SkipTests
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if (-not $SoapUiHome) {
    $candidates = @(Get-ChildItem -Path "$env:ProgramFiles\SmartBear\SoapUI*", "$env:ProgramFiles\SoapUI*" -Directory -ErrorAction SilentlyContinue)
    $SoapUiHome = $candidates | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\soapui-5.10.0.jar') } | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $SoapUiHome -or -not (Test-Path -LiteralPath (Join-Path $SoapUiHome 'bin\soapui-5.10.0.jar'))) {
    throw 'Specify -SoapUiHome pointing to SoapUI Open Source 5.10.0 (the folder containing bin and lib).'
}
if (-not $JdkHome) {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { $JdkHome = $env:JAVA_HOME }
    else {
        $jdks = @(Get-ChildItem -Path "$env:ProgramFiles\Java\jdk*", "$env:ProgramFiles\Eclipse Adoptium\jdk*" -Directory -ErrorAction SilentlyContinue)
        $JdkHome = $jdks | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $JdkHome) { throw 'Specify -JdkHome for a JDK 17 or newer. A JRE alone cannot build the plugin.' }
$javac = Join-Path $JdkHome 'bin\javac.exe'
$jar = Join-Path $JdkHome 'bin\jar.exe'
$java = Join-Path $JdkHome 'bin\java.exe'
$root = $PSScriptRoot
$classes = Join-Path $root 'build\classes'
$testClasses = Join-Path $root 'build\test-classes'
$dist = Join-Path $root 'dist'
$temporary = Join-Path $root 'build\tmp'
$resources = Join-Path $root 'build\resources'
$notices = Join-Path $resources 'META-INF'
# Remove compiled output and copied resources so deleted classes/notices cannot
# leak into a later build or release. Keep test profiles and screenshots separate.
foreach ($directory in $classes,$testClasses,$resources) {
    $resolved = [IO.Path]::GetFullPath($directory)
    $buildRoot = [IO.Path]::GetFullPath((Join-Path $root 'build')) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolved.StartsWith($buildRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe build cleanup path.' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
New-Item -ItemType Directory -Path $classes,$testClasses,$dist,$temporary,$notices -Force | Out-Null
$cp = (Join-Path $SoapUiHome 'bin\soapui-5.10.0.jar') + ';' + (Join-Path $SoapUiHome 'lib\*')
$sources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName)
& $javac --release 17 -encoding UTF-8 -classpath $cp -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
$output = Join-Path $dist 'soapui-system-theme-0.3.1.jar'
foreach ($noticeName in 'LICENSE','THIRD_PARTY_NOTICES.md') {
    Copy-Item -LiteralPath (Join-Path $root $noticeName) -Destination $notices -Force
}
Copy-Item -LiteralPath (Join-Path $root 'licenses') -Destination $notices -Recurse -Force
& $jar "-J-Djava.io.tmpdir=$temporary" --create --file $output -C $classes . -C $resources .
if ($LASTEXITCODE -ne 0) { throw 'Packaging failed.' }
if (-not $SkipTests) {
    $tests = @(Get-ChildItem -LiteralPath (Join-Path $root 'src\test\java') -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName)
    & $javac --release 17 -encoding UTF-8 -classpath "$classes;$cp" -d $testClasses @tests
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    # JGoodies 2.2 reads screen DPI even for offscreen rendering; a Windows desktop session is required.
    $testJava = Join-Path $SoapUiHome 'jre\bin\java.exe'
    if (-not (Test-Path -LiteralPath $testJava)) { $testJava = $java }
    & $testJava '-Djava.awt.headless=false' "-Djava.io.tmpdir=$temporary" "-Duser.home=$(Join-Path $root 'build\test-home')" -classpath "$testClasses;$classes;$cp" nl.vincentvonk.soapui.theme.ThemeTests (Join-Path $root 'build')
    if ($LASTEXITCODE -ne 0) { throw 'Theme tests failed.' }
    & $testJava '-Djava.awt.headless=false' "-Duser.home=$(Join-Path $root 'build\test-home')" -classpath "$testClasses;$classes;$cp" nl.vincentvonk.soapui.theme.LegacyPaintTests
    if ($LASTEXITCODE -ne 0) { throw 'Legacy toolbar and renderer tests failed.' }
    & $testJava '-Djava.awt.headless=false' "-Duser.home=$(Join-Path $root 'build\test-home')" -classpath "$testClasses;$classes;$cp" nl.vincentvonk.soapui.theme.TitleControlTests
    if ($LASTEXITCODE -ne 0) { throw 'Internal-window control tests failed.' }
    & $testJava '-Djava.awt.headless=false' "-Duser.home=$(Join-Path $root 'build\test-home')" -classpath "$testClasses;$classes;$cp" nl.vincentvonk.soapui.theme.DialogThemeTests (Join-Path $root 'build\dialogs')
    if ($LASTEXITCODE -ne 0) { throw 'Dialog theme tests failed.' }
    & $testJava '-Djava.awt.headless=false' "-Djava.io.tmpdir=$temporary" "-Duser.home=$(Join-Path $root 'build\test-home')" -classpath "$testClasses;$classes;$cp" nl.vincentvonk.soapui.theme.WebThemeTests (Join-Path $root 'build\web')
    if ($LASTEXITCODE -ne 0) { throw 'Web theme tests failed.' }
    & $testJava '-Djava.awt.headless=false' "-Duser.home=$(Join-Path $root 'build\test-home')" -classpath "$testClasses;$classes;$cp" nl.vincentvonk.soapui.theme.ControllerTests (Join-Path $root 'build')
    if ($LASTEXITCODE -ne 0) { throw 'Automatic switching integration tests failed.' }
    & $testJava '-Djava.awt.headless=true' "-Djava.io.tmpdir=$temporary" "-Duser.home=$(Join-Path $root 'build\test-home')" -classpath "$testClasses;$cp" com.eviware.soapui.plugins.PluginLoadTest $output
    if ($LASTEXITCODE -ne 0) { throw 'SoapUI plugin-loader integration test failed.' }
    & (Join-Path $root 'test-ui.ps1') -SoapUiHome $SoapUiHome
}
Write-Host "Built: $output"
