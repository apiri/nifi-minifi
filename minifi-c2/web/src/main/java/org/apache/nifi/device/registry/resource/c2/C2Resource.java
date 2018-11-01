package org.apache.nifi.device.registry.resource.c2;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.device.registry.GarconConfiguration;
import org.apache.nifi.device.registry.resource.c2.core.C2Payload;
import org.apache.nifi.device.registry.resource.c2.core.C2Response;
import org.apache.nifi.device.registry.resource.c2.core.config.C2DeviceFlowFileConfig;
import org.apache.nifi.device.registry.resource.c2.core.device.DeviceInfo;
import org.apache.nifi.device.registry.resource.c2.core.device.NetworkInfo;
import org.apache.nifi.device.registry.resource.c2.core.device.SystemInfo;
import org.apache.nifi.device.registry.resource.c2.core.metrics.C2Metrics;
import org.apache.nifi.device.registry.resource.c2.core.metrics.C2ProcessMetrics;
import org.apache.nifi.device.registry.resource.c2.core.metrics.C2QueueMetrics;
import org.apache.nifi.device.registry.resource.c2.core.metrics.pm.C2CPUMetrics;
import org.apache.nifi.device.registry.resource.c2.core.metrics.pm.C2MemoryMetrics;
import org.apache.nifi.device.registry.resource.c2.core.ops.C2Operation;
import org.apache.nifi.device.registry.resource.c2.core.state.C2State;
import org.apache.nifi.device.registry.resource.c2.dao.C2ComponentDAO;
import org.apache.nifi.device.registry.resource.c2.dao.C2DeviceDAO;
import org.apache.nifi.device.registry.resource.c2.dao.C2DeviceFlowFileConfigDAO;
import org.apache.nifi.device.registry.resource.c2.dao.C2DeviceFlowFileConfigMappingDAO;
import org.apache.nifi.device.registry.resource.c2.dao.C2HeartbeatDAO;
import org.apache.nifi.device.registry.resource.c2.dao.C2OperationDAO;
import org.apache.nifi.device.registry.resource.c2.dao.C2ProcessMetricsDAO;
import org.apache.nifi.device.registry.resource.c2.dao.C2QueueMetricsDAO;
import org.apache.nifi.device.registry.resource.c2.dto.CreateOperationRequest;
import org.apache.nifi.device.registry.resource.c2.dto.SupportedOperations;
import org.apache.nifi.device.registry.resource.c2.service.C2Service;
import org.apache.nifi.device.registry.resource.c2.service.impl.C2ServiceImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 7/7/17.
 */

@Path("/api/v1/c2")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class C2Resource {

    private static final Logger logger = LoggerFactory.getLogger(C2Resource.class);

    private GarconConfiguration configuration;
    private static ObjectMapper mapper;
    private C2Service c2Service;

    public C2Resource(GarconConfiguration conf, C2DeviceDAO c2DeviceDAO, C2QueueMetricsDAO c2QueueMetricsDAO, C2HeartbeatDAO c2HeartbeatDAO,
                      C2OperationDAO c2OperationDAO, C2ProcessMetricsDAO c2ProcessMetricsDAO, C2ComponentDAO c2ComponentDAO, C2DeviceFlowFileConfigDAO c2DeviceFlowFileConfig, C2DeviceFlowFileConfigMappingDAO c2DeviceFlowFileConfigMapping) {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.configuration = conf;
        this.c2Service = new C2ServiceImpl(c2DeviceDAO, c2QueueMetricsDAO,
                c2HeartbeatDAO, c2OperationDAO, c2ProcessMetricsDAO, c2ComponentDAO, c2DeviceFlowFileConfig, c2DeviceFlowFileConfigMapping);
    }

    @POST
    @Timed
    public Response minifiHeartbeat(C2Payload payload) {
        try {
            logger.error("MiNiFi CPP Heartbeat received: " + mapper.writeValueAsString(payload));
            System.out.println("MiNiFi CPP Heartbeat received: " + mapper.writeValueAsString(payload));

            C2Response response = c2Service.registerHeartBeat(payload);
            logger.error("C2Response: " + mapper.writeValueAsString(response));
            System.out.println("C2Response: " + mapper.writeValueAsString(response));
            return Response.ok(response).build();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Could not process payload provided", ex);
            logger.error("Payload: " + payload.toString());

            return Response.serverError().build();
        }
    }

    @GET
    @Timed
    @Path("/operations/supported")
    public Response getSupportedOperations() {
        if (logger.isDebugEnabled()) {
            logger.debug("Getting list of currently supported operations");
        }
        return Response.ok(new SupportedOperations()).build();
    }

    @GET
    @Timed
    @Path("/device/{deviceId}/operations")
    public Response listOperationsHistoryForDevice(@PathParam("deviceId") String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving operation history for device: " + deviceId);
        }
        List<C2Operation> ops = this.c2Service.getOperationHistoryForDevice(deviceId);
        return Response.ok(ops).build();
    }

    @POST
    @Timed
    @Path("/device/operation")
    public Response createOperationForDevice(CreateOperationRequest cor) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating opeartion for device Id: " + cor.getDeviceId());
        }
        this.c2Service.createOperationForDevice(cor);
        return Response.ok(cor).build();
    }

    @GET
    @Timed
    @Path("/device/{deviceId}/connections")
    public Response getConnectionsForDevice(@PathParam("deviceId") String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("retrieving connections for device : " + deviceId);
        }
        return Response.ok(this.c2Service.getConnectionsForDevice(deviceId)).build();
    }

    @GET
    @Timed
    @Path("/device/{deviceId}/components")
    public Response getComponentsForDevice(@PathParam("deviceId") String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("retrieving connections for device : " + deviceId);
        }
        return Response.ok(this.c2Service.getComponentsForDevice(deviceId)).build();
    }


    @POST
    @Timed
    @Path("/operation/ack")
    public Response ackOperations(String payload) {
        JSONObject jsonObject = new JSONObject(payload);
        long operationId = jsonObject.getLong("operationid");
        if (logger.isDebugEnabled()) {
            logger.debug("Acking operation with ID : " + operationId);
        }
        try {
            this.c2Service.ackOperation(operationId);
            return Response.ok().build();
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            return Response.serverError().build();
        }
    }


    @GET
    @Timed
    @Path("/device/config/{deviceId}")
    public Response getDeviceFlowFileConfiguration(@PathParam("deviceId") String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Getting latest FlowFile configuration for device: " + deviceId);
        }
        C2DeviceFlowFileConfig ffc = this.c2Service.getDeviceLatestFlowFileConfig(deviceId);
        return Response.ok(ffc).build();
    }

    @GET
    @Timed
    @Path("/device/configfile/{device_config_id}")
    public Response getFlowFile(@PathParam("device_config_id") String device_config_id) {
        if (logger.isDebugEnabled()) {
            logger.debug("Getting latest FlowFile configuration for device: " + device_config_id);
        }
        String file_contents = this.c2Service.getDeviceFlowFileConfig(device_config_id);
        return Response.ok(file_contents).build();
    }

    @GET
    @Timed
    @Path("/device{deviceId : (/deviceId)?}")
    public Response getDevice(@PathParam("deviceId") String deviceId) {
        if (logger.isDebugEnabled()) {
            if (deviceId == null) {
                logger.debug("Retrieving all devices from DB");
            } else {
                logger.debug("Retrieving device with ID: " + deviceId);
            }
        }

        return Response.ok(c2Service.getDevice(deviceId)).build();
    }


    @GET
    @Timed
    @Path("/device/{deviceId}/information")
    public Response getDeviceInformation(@PathParam("deviceId") String deviceId) {
        if (logger.isDebugEnabled()) {
            if (deviceId == null) {
                logger.debug("Retrieving all devices from DB");
            } else {
                logger.debug("Retrieving device with ID: " + deviceId);
            }
        }

        return Response.ok(c2Service.getDevice(deviceId)).build();
    }

    @GET
    @Timed
    @Path("/hud")
    public Response getC2HUD() {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving C2 HUD Metrics");
        }
        return Response.ok(this.c2Service.getC2HUD()).build();
    }

    public static void main(String[] args) throws JsonProcessingException {
        C2Payload p = new C2Payload();

        // Create the C2State object.
        C2State s = new C2State();
        //s.setRunning("running");
        s.setUptimeMilliseconds(100000l);

        // Create the Metrics object.
        C2Metrics m = new C2Metrics();

        // -- nested create queue metrics.
        C2QueueMetrics qm = new C2QueueMetrics();
        qm.setQueueMax(0l);
        qm.setQueued(0l);
        qm.setDataSizeMax(0l);
        qm.setDataSize(0l);
        List<C2QueueMetrics> qml = new ArrayList<C2QueueMetrics>();
        qml.add(qm);

        // -- nested create process metrics.
        C2ProcessMetrics pm = new C2ProcessMetrics();

        C2CPUMetrics cpm = new C2CPUMetrics();
        cpm.setInvolcs(1000);
        List<C2CPUMetrics> cpuml = new ArrayList<C2CPUMetrics>();
        cpuml.add(cpm);

        C2MemoryMetrics cmm = new C2MemoryMetrics();
        cmm.setMaxrss(1000);
        List<C2MemoryMetrics> cmml = new ArrayList<C2MemoryMetrics>();
        cmml.add(cmm);

        //pm.setCpuMetrics(cpuml);
        //pm.setMemoryMetrics(cmml);
        List<C2ProcessMetrics> pml = new ArrayList<C2ProcessMetrics>();
        pml.add(pm);

        Map<String, C2QueueMetrics> qmm = new HashMap<String, C2QueueMetrics>();
        qmm.put("GetFile/success/LogAttribute", qm);

        m.setQueueMetrics(qmm);
        //m.setProcessMetricss(pml);

        // Create the DeviceInfo object
        DeviceInfo di = new DeviceInfo();

        // nested --- create device info
        NetworkInfo ni = new NetworkInfo();
        ni.setHostname("localhost");
        ni.setDeviceid("13412341234");
        ni.setIp("127.0.1.1");
        List<NetworkInfo> nil = new ArrayList<NetworkInfo>();
        nil.add(ni);

        // nested --- create system info
        SystemInfo si = new SystemInfo();
        si.setVcores(8);
        si.setPhysicalMemory(100231l);
        si.setMachineArchitecture("x86_64");
        List<SystemInfo> sil = new ArrayList<SystemInfo>();
        sil.add(si);

        di.setSystemInfo(si);
        di.setNetworkInfo(ni);

        p.setState(s);
        p.setOperation("heartbeat");
        p.setMetrics(m);
        p.setDeviceInfo(di);

        System.out.println(mapper.writeValueAsString(p));
    }
}
