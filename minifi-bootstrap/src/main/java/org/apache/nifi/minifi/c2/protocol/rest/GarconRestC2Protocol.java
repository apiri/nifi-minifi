package org.apache.nifi.minifi.c2.protocol.rest;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.nifi.minifi.c2.C2Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class GarconRestC2Protocol extends RestC2Protocol {

    private static final Logger logger = LoggerFactory.getLogger(GarconRestC2Protocol.class);

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json");
    public static final int DEFAULT_GARCON_PORT = 8888;

    protected static final String API_ENDPOINT = "api/v1/c2";

    public GarconRestC2Protocol(String c2serverAddress, int c2ServerPort) {
        super(c2serverAddress, c2ServerPort);
    }

    @Override
    public C2Payload transmit(final C2Payload payload) {
        final RequestBody body = RequestBody.create(DEFAULT_MEDIA_TYPE, payload.getRawData());
        final Request request = new Request.Builder()
                .url(getHeartbeatEndpoint())
                .post(body)
                .build();
        try (final Response response = httpClient.newCall(request).execute()) {
            C2Payload responsePayload;
            if (payload.isRaw()) {
                final byte[] rawBody = response.body().bytes();
                logger.debug("Received response: " + new String(rawBody, StandardCharsets.UTF_8));
                responsePayload = new C2Payload(payload.getOperation(), payload.getIdentifier(), true, true);
                responsePayload.setRawData(rawBody);
            } else {
                responsePayload = new C2Payload(payload.getOperation(), payload.getIdentifier(), true, true);
            }
            return responsePayload;
        } catch (Exception e) {
            logger.error("Unable to successfully execute request", e);
            throw new RuntimeException("Could not successfully perform request", e);
        }
    }

    @Override
    public String getHeartbeatEndpoint() {
        return String.format("http://%s:%d/%s", getC2ServerAddress(), getC2ServerPort(), API_ENDPOINT);
    }

}
