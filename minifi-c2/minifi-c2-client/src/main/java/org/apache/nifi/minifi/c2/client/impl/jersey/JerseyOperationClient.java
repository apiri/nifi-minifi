package org.apache.nifi.minifi.c2.client.impl.jersey;

import org.apache.nifi.minifi.c2.client.OperationClient;

import javax.ws.rs.client.WebTarget;
import java.util.Collections;
import java.util.Map;

public class JerseyOperationClient extends AbstractJerseyClient implements OperationClient {

    private final WebTarget baseTarget;

    public JerseyOperationClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyOperationClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.baseTarget = baseTarget;
    }


}
