package org.apache.nifi.minifi.c2.api;

public enum Operations {
    /**
     * Operation used by MiNiFi C2 agents to acknowledge the receipt and execution of a C2 server requested operation
     */
    ACKNOWLEDGE,
    /**
     * Clears instance connection queues
     */
    CLEAR,
    /**
     * Currently unimplemented
     */
    DESCRIBE,
    /**
     * Provides status and operational capabilities to C2 server(s)
     */
    HEARTBEAT,
    /**
     * Updates components of the C2 agent instance or the flow configuration
     */
    UPDATE,
    /**
     * Restarts C2 agent instances
     */
    RESTART,
    /**
     * Starts components within the C2 agent instances
     */
    START,
    /**
     * Stops components within the C2 agent instances
     */
    STOP
}
