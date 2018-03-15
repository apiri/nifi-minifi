package org.apache.nifi.minifi;

import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.nar.ExtensionManager;
import org.apache.nifi.nar.NarClassLoaders;
import org.apache.nifi.nar.NarUnpacker;
import org.apache.nifi.nar.SystemBundle;
import org.apache.nifi.util.FileUtils;
import org.apache.nifi.util.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ManifestGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ManifestGenerator.class);

    public ManifestGenerator() throws ClassNotFoundException, IOException, IllegalArgumentException {
        NiFiProperties properties = NiFiProperties.createBasicNiFiProperties(null, null);

        // delete the web working dir - if the application does not start successfully
        // the web app directories might be in an invalid state. when this happens
        // jetty will not attempt to re-extract the war into the directory. by removing
        // the working directory, we can be assured that it will attempt to extract the
        // war every time the application starts.
        File webWorkingDir = properties.getWebWorkingDirectory();
        FileUtils.deleteFilesInDirectory(webWorkingDir, null, logger, true, true);
        FileUtils.deleteFile(webWorkingDir, logger, 3);


        // redirect JUL log events
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // expand the nars
        NarUnpacker.unpackNars(properties);

        // load the extensions classloaders
        NarClassLoaders.getInstance().init(properties.getFrameworkWorkingDirectory(), properties.getExtensionsWorkingDirectory());

        // load the framework classloader
        final ClassLoader frameworkClassLoader = NarClassLoaders.getInstance().getFrameworkBundle().getClassLoader();
        if (frameworkClassLoader == null) {
            throw new IllegalStateException("Unable to find the framework NAR ClassLoader.");
        }

        final Bundle systemBundle = SystemBundle.create(properties);
        final Set<Bundle> narBundles = NarClassLoaders.getInstance().getBundles();

        // discover the extensions
        ExtensionManager.discoverExtensions(systemBundle, narBundles);
        ExtensionManager.logClassLoaderMapping();
    }
}
