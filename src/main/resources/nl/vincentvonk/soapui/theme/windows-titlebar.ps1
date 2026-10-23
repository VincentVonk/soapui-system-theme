# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Vincent Vonk
# Embedded helper. Launched privately by WindowsTitleBar, scoped to its Java PID.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
if ([Environment]::OSVersion.Version.Build -lt 22000) {
    [Console]::WriteLine('UNSUPPORTED')
    exit
}
try { Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

public static class SoapUiTitleBars {
    private delegate bool EnumCallback(IntPtr window, IntPtr data);
    [DllImport("user32.dll")] private static extern bool EnumWindows(EnumCallback callback, IntPtr data);
    [DllImport("user32.dll")] private static extern uint GetWindowThreadProcessId(IntPtr window, out uint processId);
    [DllImport("user32.dll", CharSet=CharSet.Unicode)] private static extern int GetClassName(IntPtr window, StringBuilder name, int length);
    [DllImport("user32.dll", EntryPoint="GetWindowLongW")] private static extern int GetWindowLong(IntPtr window, int index);
    [DllImport("dwmapi.dll")] private static extern int DwmGetWindowAttribute(IntPtr window, uint attribute, out uint value, uint size);
    [DllImport("dwmapi.dll")] private static extern int DwmSetWindowAttribute(IntPtr window, uint attribute, ref uint value, uint size);
    private static readonly uint[] Attributes = { 20 }; // DWMWA_USE_IMMERSIVE_DARK_MODE
    private static readonly uint[] DarkValues = { 1 };
    private static readonly Dictionary<IntPtr, uint[]> Originals = new Dictionary<IntPtr, uint[]>();
    public static uint ProcessId;

    private static List<IntPtr> Windows() {
        List<IntPtr> windows = new List<IntPtr>();
        EnumWindows(delegate(IntPtr window, IntPtr ignored) {
            uint pid; GetWindowThreadProcessId(window, out pid);
            if (pid != ProcessId) return true;
            StringBuilder name = new StringBuilder(80); GetClassName(window, name, name.Capacity);
            if ((name.ToString() == "SunAwtFrame" || name.ToString() == "SunAwtDialog") &&
                (GetWindowLong(window, -16) & 0x00c00000) == 0x00c00000) windows.Add(window);
            return true;
        }, IntPtr.Zero);
        return windows;
    }
    private static uint[] Read(IntPtr window) {
        uint[] values = new uint[Attributes.Length];
        for (int i=0; i<Attributes.Length; i++)
            if (DwmGetWindowAttribute(window, Attributes[i], out values[i], 4) < 0) return null;
        return values;
    }
    private static bool Write(IntPtr window, uint[] values) {
        bool success = true;
        for (int i=0; i<Attributes.Length; i++) {
            uint value = values[i];
            if (DwmSetWindowAttribute(window, Attributes[i], ref value, 4) < 0) success = false;
        }
        return success;
    }
    public static string Apply(bool dark) {
        List<IntPtr> windows = Windows();
        foreach (IntPtr old in new List<IntPtr>(Originals.Keys))
            if (!windows.Contains(old)) Originals.Remove(old);
        int changed = 0, failed = 0;
        foreach (IntPtr window in windows) {
            uint[] original;
            if (dark) {
                if (!Originals.TryGetValue(window, out original)) {
                    original = Read(window);
                    if (original == null) { failed++; continue; }
                    Originals.Add(window, original);
                }
                if (Write(window, DarkValues)) changed++;
                else { failed++; Write(window, original); }
            } else if (Originals.TryGetValue(window, out original)) {
                if (Write(window, original)) { changed++; Originals.Remove(window); }
                else failed++;
            }
        }
        return "RESULT|" + changed + "|" + failed;
    }
    public static string Inspect() {
        List<string> result = new List<string>();
        foreach (IntPtr window in Windows()) {
            uint[] values = Read(window);
            if (values != null) result.Add(window.ToInt64() + "," + values[0]);
        }
        return "STATE|" + String.Join(";", result.ToArray());
    }
}
'@
} catch { [Console]::WriteLine('ERROR|' + $_.Exception.Message.Replace("`r",' ').Replace("`n",' ')); exit 1 }
[SoapUiTitleBars]::ProcessId = $TargetProcessId
[Console]::WriteLine('READY')
try {
    while ($null -ne ($command = [Console]::ReadLine())) {
        switch -CaseSensitive ($command) {
            'DARK' { [Console]::WriteLine([SoapUiTitleBars]::Apply($true)) }
            'LIGHT' { [Console]::WriteLine([SoapUiTitleBars]::Apply($false)) }
            'INSPECT' { [Console]::WriteLine([SoapUiTitleBars]::Inspect()) }
            default { throw 'Unknown title-bar command.' }
        }
    }
} catch { [Console]::WriteLine('ERROR|' + $_.Exception.Message.Replace("`r",' ').Replace("`n",' ')) }
finally { [SoapUiTitleBars]::Apply($false) | Out-Null }
