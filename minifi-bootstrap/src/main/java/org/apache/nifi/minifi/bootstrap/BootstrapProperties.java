package org.apache.nifi.minifi.bootstrap;

import java.util.Properties;

public class BootstrapProperties {

    public static final String C2_ROOT_CLASSES = "nifi.c2.root.classes";
    public static final String C2_CLASS_DEFINITIONS_PREFIX = "nifi.c2.root.class.definitions";
    public static final String C2_SERVER_REST_URL = "c2.rest.url";
    public static final String C2_SERVER_REST_ACKNOWLEDGEMENT_URL = "c2.rest.url.ack";
    public static final String C2_AGENT_HEARTBEAT_PERIOD = "c2.agent.heartbeat.period";
    public static final String C2_AGENT_HEARTBEAT_REPORTER_CLASSES = "c2.agent.heartbeat.reporter.classes";
    public static final String ENABLE_C2 = "nifi.c2.enable";
    public static final String C2_AGENT_REST_MINIMIZE_UPDATES = "c2.rest.heartbeat.minimize.updates";
    public static final String C2_AGENT_CLASS = "c2.agent.class";

    private Properties properties = new Properties();

    public BootstrapProperties() {
        this(null);
    }

    public BootstrapProperties(Properties properties) {
        this.properties = properties == null ? new Properties() : properties;
    }

    private String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getC2RootClasses() {
        return C2_ROOT_CLASSES;
    }

    public String getC2ClassDefinitionsPrefix() {
        return C2_CLASS_DEFINITIONS_PREFIX;
    }

    public String getC2ServerRestUrl() {
        return C2_SERVER_REST_URL;
    }

    public String getC2ServerRestAcknowledgementUrl() {
        return C2_SERVER_REST_ACKNOWLEDGEMENT_URL;
    }

    public long getC2AgentHeartbeatPeriod() {
        return Long.parseLong(getProperty(C2_AGENT_HEARTBEAT_PERIOD));
    }

    public String getC2AgentHeartbeatReporterClasses() {
        return C2_AGENT_HEARTBEAT_REPORTER_CLASSES;
    }

    public boolean isC2Enabled() {
        return Boolean.parseBoolean(getProperty(ENABLE_C2));
    }

    public String getC2AgentRestMinimizeUpdates() {
        return C2_AGENT_REST_MINIMIZE_UPDATES;
    }

    public String getC2AgentClass() {
        return getProperty(C2_AGENT_CLASS);
    }
}
