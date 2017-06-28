package org.apache.nifi.minifi.c2.api;

public enum Operation {
    HEARTBEAT("heartbeat"),
    CLEAR("clear"),
    START("start"),
    STOP("stop"),
    RELOAD("reload"),
    UPDATE("update");

    private String name;

    private Operation(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
