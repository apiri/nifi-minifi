package org.apache.nifi.minifi.bootstrap.c2;

import org.apache.nifi.minifi.c2.client.C2ProtocolClient;
import org.apache.nifi.minifi.c2.client.MiNiFiC2Client;
import org.apache.nifi.minifi.c2.client.impl.jersey.JerseyMiNiFiC2Client;

public class Heartbeater implements Runnable {

    public static void main(String[] args) {

        final Heartbeater heartbeater = new Heartbeater();
        heartbeater.run();

    }


    @Override
    public void run() {
        C2ProtocolClient c2ProtocolClient = getC2Client().getC2ProtocolClient();

    }

    protected MiNiFiC2Client getC2Client() {
        final MiNiFiC2Client c2Client = new JerseyMiNiFiC2Client.Builder().build();

        return c2Client;
    }
}
