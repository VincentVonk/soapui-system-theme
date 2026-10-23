// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Vincent Vonk

package nl.vincentvonk.soapui.theme;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Windows 11 DWM colors, without changing decorations, global settings or the EDT. */
final class WindowsTitleBar implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger("SoapUI.SystemTheme");
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "soapui-titlebar"); t.setDaemon(true); return t;
    });
    private ScheduledFuture<?> pending;
    private NativeBridge bridge;
    private boolean dark, closed;
    private volatile boolean unavailable;

    synchronized void setDark(boolean value) {
        if (closed || unavailable || !System.getProperty("os.name", "").startsWith("Windows")) return;
        dark = value;
        if (pending != null) pending.cancel(false);
        pending = worker.schedule(this::apply, 100, TimeUnit.MILLISECONDS);
    }
    synchronized void refresh() { if (dark) setDark(true); }

    private void apply() {
        boolean value;
        synchronized (this) { if (closed || unavailable) return; value = dark; }
        try {
            if (bridge == null) {
                if (!value) return;
                bridge = new NativeBridge();
                if (!bridge.supported) { bridge.close(); bridge = null; unavailable = true; return; }
            }
            // Startup can take a moment; honor any more recent menu selection.
            synchronized (this) { if (closed) return; value = dark; }
            String response = bridge.exchange(value ? "DARK" : "LIGHT");
            if (!response.matches("RESULT\\|[0-9]+\\|[0-9]+")) throw new IOException("Unexpected native response: " + response);
            // Dialogs can close between EnumWindows and a DWM call. An unsupported
            // or disappearing window must not disable styling of the main frame.
            if (!response.endsWith("|0")) LOG.fine("Some native windows could not be updated: " + response);
            if (!value) { bridge.close(); bridge = null; }
        } catch (Exception e) {
            // Failure of optional native styling must never undo the working Swing theme.
            LOG.log(Level.WARNING, "Native title-bar styling unavailable; keeping the ordinary Windows frame", e);
            unavailable = true;
            if (bridge != null) { bridge.close(); bridge = null; }
        }
    }
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        if (pending != null) pending.cancel(false);
        worker.execute(() -> { if (bridge != null) { bridge.close(); bridge = null; } });
        worker.shutdown();
    }

    /** Private stdin/stdout protocol; the helper can only style this JVM's AWT windows. */
    static final class NativeBridge implements AutoCloseable {
        private static final ScheduledExecutorService TIMEOUTS = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "soapui-titlebar-timeout"); t.setDaemon(true); return t;
        });
        final Process process;
        private final BufferedReader input;
        private final BufferedWriter output;
        final boolean supported;
        NativeBridge() throws IOException {
            String windows = System.getenv("SystemRoot");
            if (windows == null) throw new IOException("SystemRoot is unavailable");
            String script;
            try (InputStream resource = WindowsTitleBar.class.getResourceAsStream("windows-titlebar.ps1")) {
                if (resource == null) throw new IOException("Embedded title-bar helper is missing");
                script = "$TargetProcessId = [uint32]" + ProcessHandle.current().pid() + "\n"
                    + new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            }
            String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            // Absolute executable, no profile, no policy change, no user-controlled command text.
            process = new ProcessBuilder(Path.of(windows, "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString(),
                "-NoLogo", "-NoProfile", "-NonInteractive", "-OutputFormat", "Text", "-EncodedCommand", encoded).redirectErrorStream(true).start();
            input = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            try { supported = readResponse().equals("READY"); }
            catch (IOException e) { process.destroyForcibly(); throw e; }
        }
        String exchange(String command) throws IOException {
            output.write(command); output.newLine(); output.flush();
            return readResponse();
        }
        private String readResponse() throws IOException {
            ScheduledFuture<?> timeout = TIMEOUTS.schedule(() -> process.destroyForcibly(), 10, TimeUnit.SECONDS);
            try {
                String line = input.readLine();
                if (line == null) throw new IOException("Windows title-bar helper stopped or timed out");
                if (!line.equals("READY") && !line.equals("UNSUPPORTED") && !line.startsWith("RESULT|") && !line.startsWith("STATE|"))
                    throw new IOException("Windows title-bar helper failed: " + line);
                return line;
            } finally { timeout.cancel(false); }
        }
        @Override public void close() {
            // EOF restores captured attributes in the helper's finally block and exits.
            try { output.close(); }
            catch (IOException ignored) { }
            try { if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly(); }
            catch (InterruptedException e) { process.destroyForcibly(); Thread.currentThread().interrupt(); }
            try { input.close(); } catch (IOException ignored) { }
        }
    }
}
