package org.apache.nifi.minifi.c2;

import org.apache.nifi.minifi.c2.protocol.rest.GarconRestC2Protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.apache.nifi.minifi.c2.protocol.rest.GarconRestC2Protocol.DEFAULT_GARCON_PORT;

public class C2Agent implements HeartbeatReporter {

    protected final C2Protocol c2Protocol;
    protected final C2Serializer serializer;
    protected final C2Deserializer deserializer;

    protected final List<C2Payload> responses = new ArrayList<>();
    protected final List<C2Payload> requests = new ArrayList<>();

    public C2Agent() {
        this.c2Protocol = new GarconRestC2Protocol("localhost", DEFAULT_GARCON_PORT);
        this.serializer = new C2Serializer() {
        };
        this.deserializer = new C2Deserializer() {
        };
    }


    @Override
    public boolean sendHeartbeat() {
        // serialize request
        final String testHeartBeat = "{\n" +
                "   \"Components\" : {\n" +
                "      \"FlowController\" : \"enabled\",\n" +
                "      \"GetFile\" : \"enabled\",\n" +
                "      \"LogAttribute\" : \"enabled\"\n" +
                "   },\n" +
                "   \"DeviceInfo\" : {\n" +
                "      \"NetworkInfo\" : {\n" +
                "         \"deviceid\" : \"" + new Random().nextLong() + "\",\n" +
                "         \"hostname\" : \"" + UUID.randomUUID().toString() + "\",\n" +
                "         \"ip\" : \"\"\n" +
                "      },\n" +
                "      \"SystemInformation\" : {\n" +
                "         \"machinearch\" : \"x86_64\",\n" +
                "         \"physicalmem\" : \"17179869184\",\n" +
                "         \"vcores\" : \"8\"\n" +
                "      }\n" +
                "   },\n" +
                "   \"metrics\" : {\n" +
                "      \"ProcessMetrics\" : {\n" +
                "         \"CpuMetrics\" : {\n" +
                "            \"involcs\" : \"2183\"\n" +
                "         },\n" +
                "         \"MemoryMetrics\" : {\n" +
                "            \"maxrss\" : \"17866752\"\n" +
                "         }\n" +
                "      },\n" +
                "      \"QueueMetrics\" : {\n" +
                "         \"GetFile/success/LogAttribute\" : {\n" +
                "            \"datasize\" : \"0\",\n" +
                "            \"datasizemax\" : \"1073741824\",\n" +
                "            \"queued\" : \"0\",\n" +
                "            \"queuedmax\" : \"10000\"\n" +
                "         }\n" +
                "      },\n" +
                "      \"RepositoryMetrics\" : {\n" +
                "         \"flowfilerepository\" : {\n" +
                "            \"full\" : \"0\",\n" +
                "            \"running\" : \"1\",\n" +
                "            \"size\" : \"0\"\n" +
                "         },\n" +
                "         \"provenancerepository\" : {\n" +
                "            \"full\" : \"0\",\n" +
                "            \"running\" : \"1\",\n" +
                "            \"size\" : \"0\"\n" +
                "         }\n" +
                "      }\n" +
                "   },\n" +
                "   \"operation\" : \"heartbeat\",\n" +
                "   \"state\" : {\n" +
                "      \"running\" : \"true\",\n" +
                "      \"uptime\" : \"2919\"\n" +
                "   }\n" +
                "}";

        C2Payload payload = new C2Payload(Operation.HEARTBEAT, UUID.randomUUID().toString(), false, true);
        payload.setRawData(testHeartBeat.getBytes(StandardCharsets.UTF_8));

        // perform request
        c2Protocol.transmit(payload);

        // deserialize response

        return true;
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

    public static void main(String[] args) {
        C2Agent c2Agent = new C2Agent();
        for (int i = 0; i < 10; i++) {
            c2Agent.sendHeartbeat();
        }
    }

}
