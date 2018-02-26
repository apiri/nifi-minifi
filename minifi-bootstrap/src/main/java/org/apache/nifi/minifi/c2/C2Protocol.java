package org.apache.nifi.minifi.c2;

import org.apache.nifi.minifi.c2.client.MiNiFiC2Exception;

public interface C2Protocol {

    C2Payload transmit(C2Payload payload) throws MiNiFiC2Exception;

}
