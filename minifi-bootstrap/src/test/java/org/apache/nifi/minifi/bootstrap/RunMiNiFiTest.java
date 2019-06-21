package org.apache.nifi.minifi.bootstrap;

import org.apache.nifi.minifi.commons.schema.SecurityPropertiesSchema;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class RunMiNiFiTest {

    @Test
    public void buildSecurityPropertiesNotDefined() throws Exception {
        final Properties bootstrapProperties = getTestBootstrapProperties("bootstrap.conf.default");
        final SecurityPropertiesSchema securityPropertiesSchema = RunMiNiFi.buildSecurityPropertiesFromBootstrap(bootstrapProperties);
        Assert.assertNull(securityPropertiesSchema);
    }

    @Test
    public void buildSecurityPropertiesDefined() throws Exception {
        final Properties bootstrapProperties = getTestBootstrapProperties("bootstrap.conf.configured");
        final SecurityPropertiesSchema securityPropertiesSchema = RunMiNiFi.buildSecurityPropertiesFromBootstrap(bootstrapProperties);
        Assert.assertNotNull(securityPropertiesSchema);
    }

    private static Properties getTestBootstrapProperties(final String fileName) throws IOException {
        final Properties bootstrapProperties = new Properties();
        try (final InputStream fis = RunMiNiFiTest.class.getClassLoader().getResourceAsStream(fileName)) {
            bootstrapProperties.load(fis);
        }
        return bootstrapProperties;
    }

}