package org.apache.nifi.minifi.bootstrap.c2;

import com.hortonworks.minifi.c2.client.C2Client;
import com.hortonworks.minifi.c2.client.ProtocolClient;
import com.hortonworks.minifi.c2.client.impl.jersey.JerseyC2Client;


public class Heartbeater implements Runnable {

    public static void main(String[] args) {

        final Heartbeater heartbeater = new Heartbeater();
        heartbeater.run();

    }

    @Override
    public void run() {
        ProtocolClient c2ProtocolClient = getC2Client().getProtocolClient();
    }

    protected C2Client getC2Client() {
        final C2Client c2Client = new JerseyC2Client.Builder().build();
        return c2Client;
    }
}
