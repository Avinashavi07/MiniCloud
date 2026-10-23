package com.minicloud.infrastructure.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs a fixed argument list. Callers must never interpolate untrusted input
 * into the command array.
 */
public final class ControlledProcess {

    private static final Logger log = LoggerFactory.getLogger(ControlledProcess.class);

    private ControlledProcess() {
    }

    public static CommandResult run(long timeoutSeconds, String... command) {
        return run(timeoutSeconds, List.of(command));
    }

    public static CommandResult run(long timeoutSeconds, List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command must not be empty");
        }
        log.debug("Executing controlled process: {}", String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Controlled process timed out: {}", command.getFirst());
                return new CommandResult(-1, "", "Process timed out", true);
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), stdout.trim(), stderr.trim(), false);
        } catch (IOException ex) {
            log.debug("Controlled process failed to start {}: {}", command.getFirst(), ex.getMessage());
            return new CommandResult(-1, "", ex.getMessage() == null ? "Failed to start process" : ex.getMessage(), false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new CommandResult(-1, "", "Process interrupted", false);
        }
    }
}
