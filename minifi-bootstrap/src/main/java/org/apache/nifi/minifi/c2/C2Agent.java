package org.apache.nifi.minifi.c2;

import java.util.Set;

public interface C2Agent extends HeartbeatReporter {

    void performHeartbeat();

    Set<String> getFunctions();

    boolean setMetrics();

    void configure();

    void serializeMetrics();

    void extractPayload();

    void enqueueC2ServerResponse();

    void enqueueC2Response();

    void handleC2ServerResponse();

    void handleUpdate();

    void handle_describe();

}
