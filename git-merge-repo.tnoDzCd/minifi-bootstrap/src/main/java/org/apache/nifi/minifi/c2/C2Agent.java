package org.apache.nifi.minifi.c2;

import net.minidev.json.parser.JSONParser;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.nifi.minifi.c2.client.MiNiFiC2Exception;
import org.apache.nifi.minifi.c2.model.C2HeartbeatResponse;
import org.apache.nifi.minifi.c2.protocol.rest.GarconRestC2Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.nifi.minifi.c2.protocol.rest.GarconRestC2Protocol.DEFAULT_C2_SERVER_PORT;

public class C2Agent extends ScheduledThreadPoolExecutor implements HeartbeatReporter {
    private final Logger logger = LoggerFactory.getLogger(C2Agent.class);

    private final AtomicReference<ScheduledFuture> execScheduledFutureRef = new AtomicReference<>();
    private static final long DEFAULT_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(2);

    protected static final int DEFAULT_THREADPOOL_SIZE = 2;

    protected final C2Protocol c2Protocol;
    protected final C2Serializer serializer;
    protected final C2Deserializer deserializer;

    private final List<ScheduledFuture> scheduledTasks = new ArrayList<>();

    /* Prefer the handling of responses first before initiating new heartbeats */
//    protected final DelayQueue<QueuedPayload> payloads = new DelayQueue<>();
    protected final DelayQueue<QueuedPayload> requests = new DelayQueue<>();

    //    protected final Queue<C2Payload> requests = new ConcurrentLinkedQueue<>();
    protected final Queue<C2Payload> responses = new ConcurrentLinkedQueue<>();

    public C2Agent() {
        this(DEFAULT_THREADPOOL_SIZE,
                new GarconRestC2Protocol("localhost", DEFAULT_C2_SERVER_PORT),
                new C2Serializer() {
                },
                new C2Deserializer() {
                }
        );
    }

    public C2Agent(final int threadpoolSize, final C2Protocol protocol, final C2Serializer serializer, final C2Deserializer deserializer) {
        super(threadpoolSize);
        this.c2Protocol = protocol;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }


    @Override
    public boolean sendHeartbeat() {
        logger.debug("Enqueuing heartbeat");
        // serialize request
        final String testHeartBeat = "{\n" +
                "  \"operation\": \"heartbeat\",\n" +
                "  \"agentInfo\": {\n" +
                "    \"agentManifest\": {\n" +
                "      \"buildInfo\": {\n" +
                "        \"compiler\": \"/usr/local/opt/ccache/libexec/c++\",\n" +
                "        \"flags\": \" -std=c++11 -DOPENSSL_SUPPORT\",\n" +
                "        \"revision\": \"8ddaea0798d3d012e5c77dfee799a8443e9b7079\",\n" +
                "        \"timestamp\": 1538161487,\n" +
                "        \"version\": \"0.6.0\"\n" +
                "      },\n" +
                "      \"bundles\": [\n" +
                "        {\n" +
                "          \"artifact\": \"minifi-expression-language-extensions\",\n" +
                "          \"group\": \"org.apache.nifi.minifi\",\n" +
                "          \"version\": \"0.6.0\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"artifact\": \"minifi-http-curl\",\n" +
                "          \"group\": \"org.apache.nifi.minifi\",\n" +
                "          \"version\": \"0.6.0\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"artifact\": \"minifi-rocksdb-repos\",\n" +
                "          \"group\": \"org.apache.nifi.minifi\",\n" +
                "          \"version\": \"0.6.0\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"artifact\": \"minifi-archive-extensions\",\n" +
                "          \"group\": \"org.apache.nifi.minifi\",\n" +
                "          \"version\": \"0.6.0\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"artifact\": \"minifi-script-extensions\",\n" +
                "          \"group\": \"org.apache.nifi.minifi\",\n" +
                "          \"version\": \"0.6.0\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"schedulingDefaults\": {\n" +
                "        \"defaultMaxConcurrentTasks\": 1,\n" +
                "        \"defaultRunDurationNanos\": 0,\n" +
                "        \"defaultSchedulingPeriodMillis\": 1000,\n" +
                "        \"defaultSchedulingStrategy\": \"TIMER_DRIVEN\",\n" +
                "        \"penalizationPeriodMillis\": 30000,\n" +
                "        \"yieldDurationMillis\": 1000\n" +
                "      },\n" +
                "      \"agentType\": \"java\",\n" +
                "      \"identifier\": \"dockertesting\"\n" +
                "    },\n" +
                "    \"status\": {\n" +
                "      \"components\": {\n" +
                "        \"FlowController\": {\n" +
                "          \"running\": true\n" +
                "        },\n" +
                "        \"LogAttribute\": {\n" +
                "          \"running\": true\n" +
                "        },\n" +
                "        \"invoke\": {\n" +
                "          \"running\": true\n" +
                "        }\n" +
                "      },\n" +
                "      \"repositories\": {\n" +
                "        \"ff\": {\n" +
                "          \"size\": 0\n" +
                "        },\n" +
                "        \"repo_name\": {\n" +
                "          \"size\": 0\n" +
                "        }\n" +
                "      },\n" +
                "      \"uptime\": 1425\n" +
                "    },\n" +
                "    \"agentClass\": \"javadefault2\",\n" +
                "    \"identifier\": \"125225c6-c348-11ed-9e2e-03d5905ded96\"\n" +
                "  },\n" +
                "  \"deviceInfo\": {\n" +
                "    \"networkInfo\": {\n" +
                "      \"hostname\": \"HW12151\",\n" +
                "      \"ipAddress\": \"10.200.31.9\"\n" +
                "    },\n" +
                "    \"systemInfo\": {\n" +
                "      \"machinearch\": \"x86_64\",\n" +
                "      \"physicalMem\": 17179869184,\n" +
                "      \"vCores\": \"8\"\n" +
                "    },\n" +
                "    \"identifier\": \"17351833171120873589\"\n" +
                "  },\n" +
                "  \"flowInfo\": {\n" +
                "    \"components\": {\n" +
                "      \"FlowController\": {\n" +
                "        \"running\": true\n" +
                "      },\n" +
                "      \"LogAttribute\": {\n" +
                "        \"running\": true\n" +
                "      },\n" +
                "      \"invoke\": {\n" +
                "        \"running\": true\n" +
                "      }\n" +
                "    },\n" +
                "    \"queues\": {\n" +
                "      \"TransferFilesToRPG\": {\n" +
                "        \"dataSize\": 0,\n" +
                "        \"dataSizeMax\": 1048576,\n" +
                "        \"size\": 0,\n" +
                "        \"sizeMax\": 0\n" +
                "      },\n" +
                "      \"TransferFilesToRPG2\": {\n" +
                "        \"dataSize\": 0,\n" +
                "        \"dataSizeMax\": 1048576,\n" +
                "        \"size\": 0,\n" +
                "        \"sizeMax\": 0\n" +
                "      }\n" +
                "    },\n" +
                "    \"versionedFlowSnapshotURI\": {\n" +
                "      \"bucketId\": \"default\",\n" +
                "      \"flowId\": \"1251db52-3ba8-19df-9e2c-03d5905ded96\"\n" +
                "    },\n" +
                "    \"flowId\": \"1251db52-3ba8-19df-9e2c-03d5905ded96\"\n" +
                "  }\n" +
                "}";

        C2Payload payload = new C2Payload(Operation.HEARTBEAT, UUID.randomUUID().toString(), false, true);
        payload.setRawData(testHeartBeat.getBytes(StandardCharsets.UTF_8));

        requests.offer(new QueuedPayload(payload));
        return true;
    }

    @Override
    public boolean setInterval() {
        return false;
    }


    public void processQueues() {
        logger.warn("Processing queues");
        logger.debug("Processing queues.  Running? = ");
        logger.debug("Task count is " + getFutures().size());
        // Make preference for queued responses before additional requests

        logger.info("Currently there are {} responses enqueued.", this.responses.size());
        logger.warn("Currently there are {} requests enqueued.", this.requests.size());

        C2Payload response = null;
        while ((response = responses.poll()) != null) {
            logger.debug("Handling response {}", response.getIdentifier());
            byte[] rawData = response.getRawData();


            JSONParser jsonParser = new JSONParser(JSONParser.MODE_PERMISSIVE);
            try {
                C2HeartbeatResponse heartbeatResponse = jsonParser.parse(rawData, C2HeartbeatResponse.class);

            } catch (Exception pe) {
                logger.error("Could not successfully parse object {}", new String(rawData, StandardCharsets.UTF_8), pe);
            }
        }


        C2Payload payload = null;
        while ((payload = requests.poll()) != null) {
            logger.info("Producing C2 messages");
            logger.info("Current # of messages enqueued: " + this.requests.size());

            if (payload.isResponse()) {



            } else {
                try {
                    final C2Payload responsePayload = c2Protocol.transmit(payload);
                    responses.offer(responsePayload);
                    logger.debug("enqueuing response, total number of responses is {}", responses.size());
                } catch (MiNiFiC2Exception me) {
                    logger.error("Unable to successfully transmit a payload to the configured server. Requeuing payload.", me);
                    this.requests.add(new QueuedPayload(payload, DEFAULT_DELAY_MILLIS));
                }
            }
        }

        logger.info("After processing queues, there are {} responses and {} requests enqueued.", this.responses.size(), this.requests.size());

    }

    public void consumeC2() {
        logger.debug("Consuming C2 messages");
        logger.debug("enqueuing response, total number of responses is {}", responses.size());
    }

    public void produceC2() {
        logger.debug("Producing C2 messages");
        logger.debug("Current # of messages enqueued: " + this.requests.size());
        C2Payload payload = this.requests.poll();
        if (payload == null) {
            return;
        }

        if (payload.isResponse()) {
            logger.debug("handling response");
        } else {
            try {
                final C2Payload responsePayload = c2Protocol.transmit(payload);
                responses.offer(responsePayload);
                logger.debug("enqueuing response, total number of responses is {}", responses.size());
            } catch (MiNiFiC2Exception me) {
                logger.error("Unable to successfully transmit a payload to the configured server. Requeuing payload.", me);
                this.requests.add(new QueuedPayload(payload, DEFAULT_DELAY_MILLIS));
            }
        }
    }

    @Override
    public boolean start() {
        addFuture(this.scheduleAtFixedRate(() -> sendHeartbeat(), 0, 15, TimeUnit.SECONDS));
        for (int i = 0; i < 1; i++) {
            addFuture(this.scheduleAtFixedRate(
                    () -> this.processQueues(),
                    0, 10, TimeUnit.SECONDS));
        }
        return false;
    }

    protected synchronized void addFuture(ScheduledFuture future) {
        this.scheduledTasks.add(future);
    }

    protected synchronized List<Future> getFutures() {
        return Collections.unmodifiableList(this.scheduledTasks);
    }

    @Override
    public boolean stop() {
        this.shutdown();
        return true;
    }

    @Override
    public boolean isRunning() {
        return !(execScheduledFutureRef.get().isDone() && execScheduledFutureRef.get().isCancelled());
    }

    public static void main(String[] args) {
        C2Agent c2Agent = new C2Agent();
        c2Agent.start();
    }


    private class QueuedPayload extends C2Payload implements Delayed {

        final long delayMillis;

        public QueuedPayload(final C2Payload payload) {
            this(payload, 0);
        }

        public QueuedPayload(final C2Payload payload, final long delayMillis) {
            super(payload);
            this.delayMillis = delayMillis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return new CompareToBuilder().append(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS)).build();
        }
    }

}
