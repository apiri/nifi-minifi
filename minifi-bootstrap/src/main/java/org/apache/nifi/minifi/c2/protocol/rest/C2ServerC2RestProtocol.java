package org.apache.nifi.minifi.c2.protocol.rest;

import org.apache.nifi.minifi.c2.C2Payload;
import org.apache.nifi.minifi.c2.client.MiNiFiC2Client;
import org.apache.nifi.minifi.c2.client.impl.jersey.JerseyMiNiFiC2Client;

public class C2ServerC2RestProtocol extends RestC2Protocol {

    private final MiNiFiC2Client c2Client;

    public C2ServerC2RestProtocol(String c2serverAddress, int c2ServerPort) {
        super(c2serverAddress, c2ServerPort);
        c2Client = new JerseyMiNiFiC2Client.Builder().config(null).build();
    }


    @Override
    protected String getHeartbeatEndpoint() {
        c2Client.getC2ProtocolClient();
        return null;
    }

    @Override
    public C2Payload transmit(C2Payload payload) {
        return null;
    }
}
