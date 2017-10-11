package org.apache.nifi.minifi.c2.api;

public interface HeartbeatConsumer {

    /**
     * Primary method responsible for consuming a heartbeat provided by an instance.
     *
     * @return
     */
    CommunicationResponse consumeHeartbeat();

    /**
     *
     * @return
     */
    CommunicationResponse getSupportedOperations();

}
