// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only Windows app preference. Never writes the registry or changes a schedule. */
final class WindowsAppTheme {
    private static final Pattern VALUE = Pattern.compile(
        "(?im)^\\s*AppsUseLightTheme\\s+REG_DWORD\\s+(0x[0-9a-f]+|[0-9]+)\\s*$");

    static Optional<Boolean> parse(String output) {
        Matcher m = VALUE.matcher(output);
        if (!m.find()) return Optional.empty();
        try {
            long value = Long.decode(m.group(1));
            return value == 0 ? Optional.of(true) : value == 1 ? Optional.of(false) : Optional.empty();
        } catch (NumberFormatException e) { return Optional.empty(); }
    }

    Optional<Boolean> read() throws IOException, InterruptedException {
        String windows = System.getenv("SystemRoot");
        if (windows == null) throw new IOException("SystemRoot is unavailable");
        // Absolute executable path; no shell, PowerShell profile, network, or PATH lookup.
        Process process = new ProcessBuilder(Path.of(windows, "System32", "reg.exe").toString(),
            "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme").redirectErrorStream(true).start();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) throw new IOException("Windows theme query timed out");
            if (process.exitValue() != 0) return Optional.empty();
            return parse(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
