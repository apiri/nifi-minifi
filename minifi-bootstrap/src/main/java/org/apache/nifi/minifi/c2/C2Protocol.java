package org.apache.nifi.minifi.c2;

public interface C2Protocol {

    C2Payload transmit(C2Payload payload);

}
