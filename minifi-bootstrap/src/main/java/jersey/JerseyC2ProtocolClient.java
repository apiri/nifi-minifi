package jersey;

import org.apache.nifi.minifi.c2.client.C2ProtocolClient;

import javax.ws.rs.client.WebTarget;
import java.util.Collections;
import java.util.Map;

public class JerseyC2ProtocolClient extends AbstractJerseyClient implements C2ProtocolClient {

    private final WebTarget baseTarget;

    public JerseyC2ProtocolClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyC2ProtocolClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.baseTarget = baseTarget;
    }

}
