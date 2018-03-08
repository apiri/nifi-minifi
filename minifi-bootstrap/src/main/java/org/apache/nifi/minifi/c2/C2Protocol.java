package org.apache.nifi.minifi.c2;

public interface C2Protocol {

    void transmit(C2Payload payload);

}
