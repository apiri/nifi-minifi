package org.apache.nifi.minifi.c2.rest;

import org.apache.nifi.minifi.c2.C2Agent;

import java.util.Set;

public class RestC2Agent implements C2Agent {
    private final String c2ServerAddress;
    private final int c2ServerPort;

    public RestC2Agent(final String c2serverAddress,
                       final int c2ServerPort) {
        this.c2ServerAddress = c2serverAddress;
        this.c2ServerPort = c2ServerPort;
    }

    @Override
    public boolean sendHeartbeat() {
        return false;
    }

    @Override
    public boolean setInterval() {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void performHeartbeat() {

    }

    @Override
    public Set<String> getFunctions() {
        return null;
    }

    @Override
    public boolean setMetrics() {
        return false;
    }

    @Override
    public void configure() {

    }

    @Override
    public void serializeMetrics() {

    }

    @Override
    public void extractPayload() {

    }

    @Override
    public void enqueueC2ServerResponse() {

    }

    @Override
    public void enqueueC2Response() {

    }

    @Override
    public void handleC2ServerResponse() {

    }

    @Override
    public void handleUpdate() {

    }

    @Override
    public void handle_describe() {

    }

}
