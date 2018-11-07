package jersey;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.minifi.c2.client.AgentClassClient;
import org.apache.nifi.minifi.c2.client.MiNiFiC2Exception;
import org.apache.nifi.minifi.c2.model.AgentClass;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JerseyAgentClassClient extends AbstractJerseyClient implements AgentClassClient {

    private static final String AGENT_CLASSES_TARGET_PATH = "/agent-classes";

    private final WebTarget agentClassTarget;

    public JerseyAgentClassClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyAgentClassClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.agentClassTarget = baseTarget.path(AGENT_CLASSES_TARGET_PATH);
    }

    @Override
    public AgentClass createAgentClass(final AgentClass agentClass) throws MiNiFiC2Exception, IOException {

        if (agentClass == null) {
            throw new IllegalArgumentException("Missing the required parameter 'agentClass' when calling createAgentClass");
        }

        final String agentClassName = agentClass.getName();
        if (StringUtils.isBlank(agentClassName)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error creating agent class", () ->
                getRequestBuilder(agentClassTarget)
                .post(
                        Entity.entity(agentClass, MediaType.APPLICATION_JSON),
                        AgentClass.class
                )
        );
    }

    @Override
    public List<AgentClass> getAgentClasses() throws MiNiFiC2Exception, IOException {
        return executeAction("Error creating agent class", () -> {
            final AgentClass[] agentClasses = getRequestBuilder(agentClassTarget).get(AgentClass[].class);
            return agentClasses == null ? Collections.emptyList() : Arrays.asList(agentClasses);
        });
    }

    @Override
    public AgentClass getAgentClass(String className) throws MiNiFiC2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }


        return executeAction("Error retrieving agent class", () -> {
            final WebTarget target = agentClassTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).get(AgentClass.class);
        });
    }

    @Override
    public AgentClass replaceAgentClass(String className, AgentClass replacementClass) throws MiNiFiC2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }

        return executeAction("Error updating agent class", () -> {
            final WebTarget target = agentClassTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).put(Entity.entity(replacementClass, MediaType.APPLICATION_JSON),
                    AgentClass.class);
        });
    }

    @Override
    public AgentClass deleteAgentClass(String className) throws MiNiFiC2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Class name cannot be blank");
        }

        return executeAction("Error deleting flow", () -> {
            final WebTarget target = agentClassTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);

            return getRequestBuilder(target).delete(AgentClass.class);
        });
    }
}
