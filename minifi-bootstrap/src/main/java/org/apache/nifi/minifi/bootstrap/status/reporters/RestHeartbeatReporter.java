package org.apache.nifi.minifi.bootstrap.status.reporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.minifi.bootstrap.BootstrapProperties;
import org.apache.nifi.minifi.bootstrap.QueryableStatusAggregator;
import org.apache.nifi.minifi.bootstrap.configuration.ingestors.ConfigurableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
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
        this.agentMonitor = queryableStatusAggregator;


        if (!bootstrapProperties.isC2Enabled()) {
            throw new IllegalArgumentException("Cannot initialize the REST HeartbeatReporter when C2 is not enabled");
        }

        this.c2ServerUrl = bootstrapProperties.getC2ServerRestUrl();
        this.agentClass = bootstrapProperties.getC2AgentClass();

        pollingPeriodMS.set(1000);
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
//            logger.error("****************************************************************************************************************************************************");
//            logger.error("Performing heartbeat at " + new Date());
//            logger.error("****************************************************************************************************************************************************");

            try {
                String heartbeatString = generateHeartbeat();
//            try {
//                heartbeatString = objectMapper.writeValueAsString(heartbeat);
//            } catch (JsonProcessingException e) {
//
//                e.printStackTrace();
//            }
//            logger.info("Generated heartbeat {}", heartbeatString);

                final RequestBody requestBody = RequestBody.create(MediaType.parse(javax.ws.rs.core.MediaType.APPLICATION_JSON), heartbeatString);
                final Request.Builder requestBuilder = new Request.Builder()
                        .post(requestBody)
                        .url("http://localhost:10080/c2/api/c2-protocol/heartbeat");
                try {
                    Response heartbeatResponse = httpClientReference.get().newCall(requestBuilder.build()).execute();
                    final String responseBody = heartbeatResponse.body().string();
                    logger.trace("Received heartbeat response {}", responseBody);
                    ObjectMapper objMapper = new ObjectMapper();
                    JsonNode responseJsonNode = objMapper.readTree(responseBody);

                    JsonNode requestedOperations = responseJsonNode.get("requestedOperations");
                    JsonNode operation = requestedOperations.get(0);
                    if (operation.get("operation").asText().equals("UPDATE")) {
                        final String opIdentifier = operation.get("identifier").asText();
                        final JsonNode args = operation.get("args");
                        final String updateLocation = args.get("location").asText();
                        logger.trace("Will perform flow update from {} for command #{}", updateLocation, opIdentifier);

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("Could not transmit", e);

                }
            } catch (IOException e) {
                logger.error("Could not transmit", e);
            }
        }
    }


    private String generateHeartbeat() throws IOException {
        return this.agentMonitor.getBundles();
    }

    public static void main(String[] args) {
        RestHeartbeatReporter reporter = new RestHeartbeatReporter();
        reporter.setPeriod(1000);
        reporter.start();
    }

}
