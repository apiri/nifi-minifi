package org.apache.nifi.minifi.bootstrap.c2;

public class C2Heartbeater implements Runnable {
    public static void main(String[] args) {
        final C2Heartbeater heartbeater = new C2Heartbeater();
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
