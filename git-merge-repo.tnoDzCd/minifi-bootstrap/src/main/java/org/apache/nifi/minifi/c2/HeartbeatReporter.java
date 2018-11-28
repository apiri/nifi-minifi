package org.apache.nifi.minifi.c2;

public interface HeartbeatReporter {

    boolean sendHeartbeat();

    boolean setInterval();

    boolean start();

    boolean stop();

    boolean isRunning();

}
