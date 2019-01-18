package org.apache.nifi.minifi.bootstrap.status.reporters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.minifi.c2.model.AgentInfo;
import com.hortonworks.minifi.c2.model.AgentManifest;
import com.hortonworks.minifi.c2.model.AgentStatus;
import com.hortonworks.minifi.c2.model.C2Heartbeat;
import com.hortonworks.minifi.c2.model.DeviceInfo;
import com.hortonworks.minifi.c2.model.FlowInfo;
import com.hortonworks.minifi.c2.model.FlowStatus;
import com.hortonworks.minifi.c2.model.extension.ComponentManifest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.minifi.bootstrap.BootstrapProperties;
import org.apache.nifi.minifi.bootstrap.QueryableStatusAggregator;
import org.apache.nifi.minifi.bootstrap.configuration.ingestors.ConfigurableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class RestHeartbeatReporter extends HeartbeatReporter implements ConfigurableHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(RestHeartbeatReporter.class);

    private String c2ServerUrl;
    private String agentClass;
    private QueryableStatusAggregator agentMonitor;

    private ObjectMapper objectMapper;
    private final AtomicLong pollingPeriodMS = new AtomicLong();

    @Override
    public void initialize(Properties properties, QueryableStatusAggregator queryableStatusAggregator) {
        final BootstrapProperties bootstrapProperties = new BootstrapProperties(properties);
        objectMapper = new ObjectMapper();
        this.agentMonitor= queryableStatusAggregator;

        if (!bootstrapProperties.isC2Enabled()) {
            throw new IllegalArgumentException("Cannot initialize the REST HeartbeatReporter when C2 is not enabled");
        }

        this.c2ServerUrl = bootstrapProperties.getC2ServerRestUrl();
        this.agentClass = bootstrapProperties.getC2AgentClass();

        pollingPeriodMS.set(10000);
        if (pollingPeriodMS.get() < 1) {
            throw new IllegalArgumentException("Property, " + BootstrapProperties.C2_AGENT_HEARTBEAT_PERIOD + ", for the polling period ms must be set with a positive integer.");
        }
        this.setPeriod((int) pollingPeriodMS.get());


        if (StringUtils.isBlank(c2ServerUrl)) {
            throw new IllegalArgumentException("Property, " + c2ServerUrl + ", for the hostname to pull configurations from must be specified.");
        }

        httpClientReference.set(null);

        final OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

        // Set whether to follow redirects
        okHttpClientBuilder.followRedirects(false);

//        // check if the ssl path is set and add the factory if so
//        if (properties.containsKey(KEYSTORE_LOCATION_KEY)) {
//            try {
//                setSslSocketFactory(okHttpClientBuilder, properties);
//                connectionScheme = "https";
//            } catch (Exception e) {
//                throw new IllegalStateException(e);
//            }
//        } else {
//            connectionScheme = "http";
//        }

        httpClientReference.set(okHttpClientBuilder.build());
        reportRunner = new RestHeartbeatReporter.HeartbeatReporter();
    }

    private class HeartbeatReporter implements Runnable {
        @Override
        public void run() {
            logger.error("Performing heartbeat at " + new Date());

            C2Heartbeat heartbeat = generateHeartbeat();
            String heartbeatString = null;
            try {
                heartbeatString = objectMapper.writeValueAsString(heartbeat);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            logger.info("Generated heartbeat {}", heartbeatString);

            final RequestBody requestBody = RequestBody.create(MediaType.parse(javax.ws.rs.core.MediaType.APPLICATION_JSON), heartbeatString);
            final Request.Builder requestBuilder = new Request.Builder()
                    .post(requestBody)
                    .url("http://localhost:10080/c2/api/c2-protocol/heartbeat");
            try {
                httpClientReference.get().newCall(requestBuilder.build()).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private C2Heartbeat generateHeartbeat() {

        // Populate AgentInfo
        final AgentInfo agentInfo = new AgentInfo();
//        agentInfo.setAgentClass(this.agentClass);
//        if (this.agentClass == null) {
            agentInfo.setAgentClass("devclass");
//        }
        agentInfo.setIdentifier("AGENTINFOIDENTIFIER");
        final AgentStatus agentStatus = new AgentStatus();
        agentStatus.setComponents(null);
        agentStatus.setRepositories(null);
        agentStatus.setUptime(System.currentTimeMillis() / new Random().nextLong());
        agentInfo.setStatus(agentStatus);
        final AgentManifest agentManifest = new AgentManifest();
        agentManifest.setVersion("1");
        agentManifest.setIdentifier(null);
        final ComponentManifest componentManifest = new ComponentManifest();
        componentManifest.setProcessors(null);
        componentManifest.setApis(null);
        componentManifest.setControllerServices(null);
        componentManifest.setReportingTasks(null);
        agentManifest.setComponentManifest(componentManifest);
        agentInfo.setAgentManifest(agentManifest);


        Set<Bundle> bundles = new HashSet<>();
        try {
            bundles.addAll(agentMonitor.getBundles());
        }catch (IOException ioe) {
            logger.error("Could not get all bundles for instance", ioe);
        }

        logger.error("Bundles are: {}", bundles);
        // Populate DeviceInfo
        final DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setIdentifier("DEVICEINFOIDENTIFIER");
        deviceInfo.setNetworkInfo(null);
        deviceInfo.setSystemInfo(null);

        // Populate FlowInfo
        final FlowInfo flowInfo = new FlowInfo();
        flowInfo.setFlowId("flow identifer");
        FlowStatus flowStatus = new FlowStatus();
        flowStatus.setComponents(null);
        flowStatus.setQueues(null);
        flowInfo.setStatus(flowStatus);
        flowInfo.setVersionedFlowSnapshotURI(null);

        // Populate heartbeat
        final C2Heartbeat heartbeat = new C2Heartbeat();
        heartbeat.setAgentInfo(agentInfo);
        heartbeat.setDeviceInfo(deviceInfo);
        heartbeat.setFlowInfo(flowInfo);
        heartbeat.setCreated(new Date().getTime());
        heartbeat.setIdentifier("IDENTIFIER");

        return heartbeat;
    }

    public static void main(String[] args) {
        RestHeartbeatReporter reporter = new RestHeartbeatReporter();
        reporter.setPeriod(1000);
        reporter.start();
    }

}
