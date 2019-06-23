package org.apache.nifi.minifi.bootstrap;

import org.apache.nifi.minifi.commons.schema.SecurityPropertiesSchema;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class RunMiNiFiTest {

    @Test
    public void buildSecurityPropertiesNotDefined() throws Exception {
        final Properties bootstrapProperties = getTestBootstrapProperties("bootstrap.conf.default");
        final Optional<SecurityPropertiesSchema> securityPropsOptional = RunMiNiFi.buildSecurityPropertiesFromBootstrap(bootstrapProperties);
        Assert.assertTrue(!securityPropsOptional.isPresent());
    }

    @Test
    public void buildSecurityPropertiesDefined() throws Exception {
        final Properties bootstrapProperties = getTestBootstrapProperties("bootstrap.conf.configured");
        final Optional<SecurityPropertiesSchema> securityPropsOptional = RunMiNiFi.buildSecurityPropertiesFromBootstrap(bootstrapProperties);
        Assert.assertTrue(securityPropsOptional.isPresent());
    }

    private static Properties getTestBootstrapProperties(final String fileName) throws IOException {
        final Properties bootstrapProperties = new Properties();
        try (final InputStream fis = RunMiNiFiTest.class.getClassLoader().getResourceAsStream(fileName)) {
            bootstrapProperties.load(fis);
        }
        return bootstrapProperties;
    }

}