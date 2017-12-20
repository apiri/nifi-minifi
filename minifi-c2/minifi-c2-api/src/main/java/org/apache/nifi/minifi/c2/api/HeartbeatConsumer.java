package org.apache.nifi.minifi.c2.api;

import org.apache.nifi.device.registry.resource.c2.core.C2Payload;
import org.apache.nifi.device.registry.resource.c2.core.C2Response;
import org.apache.nifi.device.registry.resource.c2.core.components.Component;
import org.apache.nifi.device.registry.resource.c2.core.config.C2DeviceFlowFileConfig;
import org.apache.nifi.device.registry.resource.c2.core.device.DeviceInfo;
import org.apache.nifi.device.registry.resource.c2.core.metrics.C2QueueMetrics;
import org.apache.nifi.device.registry.resource.c2.core.ops.C2Operation;
import org.apache.nifi.device.registry.resource.c2.dto.C2HUD;
import org.apache.nifi.device.registry.resource.c2.dto.CreateOperationRequest;

import java.util.List;

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

    /**
     * Handles the C2Payload JSON object received from the MiNifi device and writes all values
     * into the underlying database.
     *
     * @param heartbeatPayload
     * @return
     */
    C2Response registerHeartBeat(C2Payload heartbeatPayload);

    /**
     * Acknowledges that the operation with the specified id was completed on the client side.
     *
     * @param operationId
     */
    void ackOperation(long operationId);

    /**
     * Creates an opeartion for the specified device.
     *
     * @param cor
     */
    void createOperationForDevice(CreateOperationRequest cor);

    /**
     * Gets the connections for a particular device.
     *
     * @param deviceId
     * @return
     */
    List<C2QueueMetrics> getConnectionsForDevice(String deviceId);

    /**
     * Gets the connections for a particular device.
     *
     * @param deviceId
     * @return
     */
    List<Component> getComponentsForDevice(String deviceId);

    /**
     * Gets the latest flowfile configuration for the minifi device.
     *
     * @param deviceId
     * @return
     */
    C2DeviceFlowFileConfig getDeviceLatestFlowFileConfig(String deviceId);

    /**
     * Gets the latest flowfile configuration for the minifi device.
     *
     * @param deviceConfigId
     * @return content of configuratin file
     */
    String getDeviceFlowFileConfig(String deviceConfigId);

    /**
     * Retrieves the operation history for a particular device. This will include pending operations.
     *
     * @param deviceId
     *  Id of the device the operation will be retrieve for.
     *
     * @return
     */
    List<C2Operation> getOperationHistoryForDevice(String deviceId);

    /**
     * Retrieves the specified device from the DB. If the DeviceID is empty
     * then all devices up to the DB return limit will be retrieved.
     *
     * @param deviceId
     * @return
     */
    List<DeviceInfo> getDevice(String deviceId);

    /**
     * Creates the metrics for the UI to display the C2 Heads Up Display
     *
     * @return
     *  C2HUD object representing the current HUD state.
     */
    C2HUD getC2HUD();

}
