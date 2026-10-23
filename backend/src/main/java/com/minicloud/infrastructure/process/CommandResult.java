package com.minicloud.infrastructure.process;

public record CommandResult(int exitCode, String stdout, String stderr, boolean timedOut) {

    public boolean succeeded() {
        return !timedOut && exitCode == 0;
    }
}
