/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.minifi.c2.client.impl.jersey;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.minifi.c2.client.AgentClassClient;
import org.apache.nifi.minifi.c2.client.AgentManifestClient;
import org.apache.nifi.minifi.c2.client.C2ProtocolClient;
import org.apache.nifi.minifi.c2.client.MiNiFiC2Client;
import org.apache.nifi.minifi.c2.client.MiNiFiC2ClientConfig;
import org.apache.nifi.minifi.c2.client.OperationClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.net.URI;

/**
 * A NiFiRegistryClient that uses Jersey Client.
 */
public class JerseyMiNiFiC2Client implements MiNiFiC2Client {

    static final String MINIFI_C2_CONTEXT = "minifi-c2-api";
    static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    static final int DEFAULT_READ_TIMEOUT = 10000;

    private final Client client;
    private final WebTarget baseTarget;

    private final AgentClassClient agentClassClient;
    private final C2ProtocolClient c2ProtocolClient;
    private final OperationClient operationClient;
    private final AgentManifestClient manifestClient;

    private JerseyMiNiFiC2Client(final MiNiFiC2Client.Builder builder) {
        final MiNiFiC2ClientConfig registryClientConfig = builder.getConfig();
        if (registryClientConfig == null) {
            throw new IllegalArgumentException("MiNiFiC2ClientConfig cannot be null");
        }

        String baseUrl = registryClientConfig.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("Base URL cannot be blank");
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (!baseUrl.endsWith(MINIFI_C2_CONTEXT)) {
            baseUrl = baseUrl + "/" + MINIFI_C2_CONTEXT;
        }

        try {
            new URI(baseUrl);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid base URL: " + e.getMessage(), e);
        }

        final SSLContext sslContext = registryClientConfig.getSslContext();
        final HostnameVerifier hostnameVerifier = registryClientConfig.getHostnameVerifier();

        final ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        if (sslContext != null) {
            clientBuilder.sslContext(sslContext);
        }
        if (hostnameVerifier != null) {
            clientBuilder.hostnameVerifier(hostnameVerifier);
        }

        final int connectTimeout = registryClientConfig.getConnectTimeout() == null ? DEFAULT_CONNECT_TIMEOUT : registryClientConfig.getConnectTimeout();
        final int readTimeout = registryClientConfig.getReadTimeout() == null ? DEFAULT_READ_TIMEOUT : registryClientConfig.getReadTimeout();

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);
        clientConfig.property(ClientProperties.READ_TIMEOUT, readTimeout);
        clientConfig.register(jacksonJaxbJsonProvider());
        clientBuilder.withConfig(clientConfig);
        this.client = clientBuilder.build();

        this.baseTarget = client.target(baseUrl);
        this.agentClassClient = new JerseyAgentClassClient(baseTarget);
        this.manifestClient = new JerseyAgentManifestClient(baseTarget);
        this.c2ProtocolClient = new JerseyC2ProtocolClient(baseTarget);
        this.operationClient = new JerseyOperationClient(baseTarget);

    }

    @Override
    public AgentClassClient getAgentClassClient() {
        return this.agentClassClient;
    }

    public AgentManifestClient getManifestClient() {
        return manifestClient;
    }

    @Override
    public C2ProtocolClient getC2ProtocolClient() {
        return this.c2ProtocolClient;
    }

    @Override
    public OperationClient getOperationClient() {
        return this.operationClient;
    }

    @Override
    public void close() throws IOException {
        if (this.client != null) {
            try {
                this.client.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Builder for creating a JerseyNiFiRegistryClient.
     */
    public static class Builder implements MiNiFiC2Client.Builder {

        private MiNiFiC2ClientConfig clientConfig;

        @Override
        public Builder config(final MiNiFiC2ClientConfig clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        @Override
        public MiNiFiC2ClientConfig getConfig() {
            return clientConfig;
        }

        @Override
        public JerseyMiNiFiC2Client build() {
            return new JerseyMiNiFiC2Client(this);
        }

    }

    private static JacksonJaxbJsonProvider jacksonJaxbJsonProvider() {
        JacksonJaxbJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(mapper.getTypeFactory()));
        // Ignore unknown properties so that deployed client remain compatible with future versions of NiFi Registry that add new fields
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();
//        module.addDeserializer(BucketItem[].class, new BucketItemDeserializer());
        mapper.registerModule(module);

        jacksonJaxbJsonProvider.setMapper(mapper);
        return jacksonJaxbJsonProvider;
    }
}