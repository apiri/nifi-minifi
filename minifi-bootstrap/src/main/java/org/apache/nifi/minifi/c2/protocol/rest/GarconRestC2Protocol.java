package org.apache.nifi.minifi.c2.protocol.rest;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.nifi.minifi.c2.C2Payload;

import java.io.IOException;

public class GarconRestC2Protocol extends RestC2Protocol {

    public static final int DEFAULT_GARCON_PORT = 8888;

    protected static final String API_ENDPOINT = "api/v1/c2";

    public GarconRestC2Protocol(String c2serverAddress, int c2ServerPort) {
        super(c2serverAddress, c2ServerPort);
    }

    @Override
    public void transmit(C2Payload payload) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), payload.getRawData());

        Request request = new Request.Builder()
                .url(getHeartbeatEndpoint())
                .post(body)
                .build();
        try {
            Response response = httpClient.newCall(request).execute();
            System.out.println("Received response: " + response.body().string());
        } catch (IOException e) {
        }
    }

    @Override
    public String getHeartbeatEndpoint() {
        return String.format("http://%s:%d/%s", getC2ServerAddress(), getC2ServerPort(), API_ENDPOINT);
    }

}
