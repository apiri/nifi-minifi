package org.apache.nifi.minifi.bootstrap.c2;

import client.AgentManifestClient;
import model.AgentManifest;
import org.apache.commons.lang3.StringUtils;


import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JerseyAgentManifestClient extends AbstractJerseyClient implements AgentManifestClient {
    private static final String AGENT_MANIFESTS_TARGET_PATH = "/agent-manifests";
    private final WebTarget agentManifestTarget;

    public JerseyAgentManifestClient(final WebTarget baseTarget) {
        this(baseTarget, Collections.emptyMap());
    }

    public JerseyAgentManifestClient(final WebTarget baseTarget, final Map<String, String> headers) {
        super(headers);
        this.agentManifestTarget = baseTarget.path(AGENT_MANIFESTS_TARGET_PATH);
    }

    public AgentManifest createAgentManifest(final AgentManifest agentManifest) throws MiNiFiC2Exception, IOException {
        return this.createAgentManifest(agentManifest, null);
    }

    @Override
    public AgentManifest createAgentManifest(final AgentManifest agentManifest, final String agentClass) throws MiNiFiC2Exception, IOException {
        if (agentManifest == null) {
            throw new IllegalArgumentException("Missing the required parameter 'agentManifest' when calling createAgentManifest");
        }
        final WebTarget manifestTarget = StringUtils.isNotBlank(agentClass) ? agentManifestTarget.queryParam("class", agentClass) : agentManifestTarget;
        return executeAction("Error creating agent manifest", () ->
                getRequestBuilder(manifestTarget)
                        .post(
                                Entity.entity(agentManifest, MediaType.APPLICATION_JSON), AgentManifest.class
                        ));
    }

    @Override
    public List<AgentManifest> getAgentManifests() throws MiNiFiC2Exception, IOException {
        return executeAction("Error creating agent class", () -> {
            final AgentManifest[] agentManifests = getRequestBuilder(agentManifestTarget).get(AgentManifest[].class);
            return agentManifests == null ? Collections.emptyList() : Arrays.asList(agentManifests);
        });
    }

    @Override
    public AgentManifest getAgentManifest(String className) throws MiNiFiC2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Agent class name cannot be blank");
        }
        return executeAction("Error retrieving agent class", () -> {
            final WebTarget target = agentManifestTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);
            return getRequestBuilder(target).get(AgentManifest.class);
        });
    }

    @Override
    public AgentManifest deleteAgentManifest(String className) throws MiNiFiC2Exception, IOException {
        if (StringUtils.isBlank(className)) {
            throw new IllegalArgumentException("Manifest name cannot be blank");
        }
        return executeAction("Error deleting flow", () -> {
            final WebTarget target = agentManifestTarget
                    .path("/{name}")
                    .resolveTemplate("name", className);
            return getRequestBuilder(target).delete(AgentManifest.class);
        });
    }
}