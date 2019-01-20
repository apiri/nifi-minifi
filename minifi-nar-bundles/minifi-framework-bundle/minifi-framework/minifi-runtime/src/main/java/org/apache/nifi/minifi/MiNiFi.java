/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.minifi;

import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.bundle.BundleCoordinate;
import org.apache.nifi.components.ConfigurableComponent;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.controller.exception.ProcessorInstantiationException;
import org.apache.nifi.nar.ExtensionManager;
import org.apache.nifi.nar.NarClassLoaders;
import org.apache.nifi.nar.NarUnpacker;
import org.apache.nifi.nar.SystemBundle;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.util.FileUtils;
import org.apache.nifi.util.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// These are from the minifi-nar-utils

public class MiNiFi {

    private static final Logger logger = LoggerFactory.getLogger(MiNiFi.class);
    private final MiNiFiServer minifiServer;
    private final BootstrapListener bootstrapListener;

    public static final String BOOTSTRAP_PORT_PROPERTY = "nifi.bootstrap.listen.port";
    private volatile boolean shutdown = false;


    public MiNiFi(final NiFiProperties properties)
            throws ClassNotFoundException, IOException, NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, FlowEnrichmentException {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                logger.error("An Unknown Error Occurred in Thread {}: {}", t, e.toString());
                logger.error("", e);
            }
        });

        // register the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                // shutdown the jetty server
                shutdownHook(true);
            }
        }));

        final String bootstrapPort = System.getProperty(BOOTSTRAP_PORT_PROPERTY);
        if (bootstrapPort != null) {
            try {
                final int port = Integer.parseInt(bootstrapPort);

                if (port < 1 || port > 65535) {
                    throw new RuntimeException("Failed to start MiNiFi because system property '" + BOOTSTRAP_PORT_PROPERTY + "' is not a valid integer in the range 1 - 65535");
                }

                bootstrapListener = new BootstrapListener(this, port);
                bootstrapListener.start();
            } catch (final NumberFormatException nfe) {
                throw new RuntimeException("Failed to start MiNiFi because system property '" + BOOTSTRAP_PORT_PROPERTY + "' is not a valid integer in the range 1 - 65535");
            }
        } else {
            logger.info("MiNiFi started without Bootstrap Port information provided; will not listen for requests from Bootstrap");
            bootstrapListener = null;
        }

        // delete the web working dir - if the application does not start successfully
        // the web app directories might be in an invalid state. when this happens
        // jetty will not attempt to re-extract the war into the directory. by removing
        // the working directory, we can be assured that it will attempt to extract the
        // war every time the application starts.
        File webWorkingDir = properties.getWebWorkingDirectory();
        FileUtils.deleteFilesInDirectory(webWorkingDir, null, logger, true, true);
        FileUtils.deleteFile(webWorkingDir, logger, 3);

        detectTimingIssues();

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

        // Enrich the flow xml using the Extension Manager mapping
        final FlowParser flowParser = new FlowParser();
        final FlowEnricher flowEnricher = new FlowEnricher(this, flowParser, properties);
        flowEnricher.enrichFlowWithBundleInformation();

        // load the server from the framework classloader
        Thread.currentThread().setContextClassLoader(frameworkClassLoader);
        Class<?> minifiServerClass = Class.forName("org.apache.nifi.minifi.MiNiFiServer", true, frameworkClassLoader);
        Constructor<?> minifiServerConstructor = minifiServerClass.getConstructor(NiFiProperties.class);

        final long startTime = System.nanoTime();
        minifiServer = (MiNiFiServer) minifiServerConstructor.newInstance(properties);

        if (shutdown) {
            logger.info("MiNiFi has been shutdown via MiNiFi Bootstrap. Will not start Controller");
        } else {
            minifiServer.start();

            if (bootstrapListener != null) {
                bootstrapListener.sendStartedStatus(true);
            }

            final long endTime = System.nanoTime();
            final long durationNanos = endTime - startTime;
            // Convert to millis for higher precision and then convert to a float representation of seconds
            final float durationSeconds = TimeUnit.MILLISECONDS.convert(durationNanos, TimeUnit.NANOSECONDS) / 1000f;
            logger.info("Controller initialization took {} nanoseconds ({} seconds).", durationNanos, String.format("%.01f", durationSeconds));
        }
    }

    protected void shutdownHook(boolean isReload) {
        try {
            this.shutdown = true;

            logger.info("Initiating shutdown of MiNiFi server...");
            if (minifiServer != null) {
                minifiServer.stop();
            }
            if (bootstrapListener != null) {
                if (isReload) {
                    bootstrapListener.reload();
                } else {
                    bootstrapListener.stop();
                }
            }
            logger.info("MiNiFi server shutdown completed (nicely or otherwise).");
        } catch (final Throwable t) {
            logger.warn("Problem occurred ensuring MiNiFi server was properly terminated due to " + t);
        }
    }

    /**
     * Determine if the machine we're running on has timing issues.
     */
    private void detectTimingIssues() {
        final int minRequiredOccurrences = 25;
        final int maxOccurrencesOutOfRange = 15;
        final AtomicLong lastTriggerMillis = new AtomicLong(System.currentTimeMillis());

        final ScheduledExecutorService service = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

            @Override
            public Thread newThread(final Runnable r) {
                final Thread t = defaultFactory.newThread(r);
                t.setDaemon(true);
                t.setName("Detect Timing Issues");
                return t;
            }
        });

        final AtomicInteger occurrencesOutOfRange = new AtomicInteger(0);
        final AtomicInteger occurrences = new AtomicInteger(0);
        final Runnable command = new Runnable() {
            @Override
            public void run() {
                final long curMillis = System.currentTimeMillis();
                final long difference = curMillis - lastTriggerMillis.get();
                final long millisOff = Math.abs(difference - 2000L);
                occurrences.incrementAndGet();
                if (millisOff > 500L) {
                    occurrencesOutOfRange.incrementAndGet();
                }
                lastTriggerMillis.set(curMillis);
            }
        };

        final ScheduledFuture<?> future = service.scheduleWithFixedDelay(command, 2000L, 2000L, TimeUnit.MILLISECONDS);

        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                future.cancel(true);
                service.shutdownNow();

                if (occurrences.get() < minRequiredOccurrences || occurrencesOutOfRange.get() > maxOccurrencesOutOfRange) {
                    logger.warn("MiNiFi has detected that this box is not responding within the expected timing interval, which may cause "
                            + "Processors to be scheduled erratically. Please see the MiNiFi documentation for more information.");
                }
            }
        };
        final Timer timer = new Timer(true);
        timer.schedule(timerTask, 60000L);
    }

    MiNiFiServer getMinifiServer() {
        return minifiServer;
    }

    /**
     * Main entry point of the application.
     *
     * @param args things which are ignored
     */
    public static void main(String[] args) {
        logger.info("Launching MiNiFi...");
        try {
            NiFiProperties niFiProperties = NiFiProperties.createBasicNiFiProperties(null, null);
            new MiNiFi(niFiProperties);
        } catch (final Throwable t) {
            logger.error("Failure to launch MiNiFi due to " + t, t);
        }
    }

    protected List<Bundle> getBundles(final String bundleClass) {
        return ExtensionManager.getBundles(bundleClass);
    }

    protected void inspectExtensionClass() {
        final Set<Class> processorExtensions = ExtensionManager.getExtensions(Processor.class);
        final Set<Class> controllerService = ExtensionManager.getExtensions(ControllerService.class);

        for (final Class<?> extensionClass : processorExtensions) {
            if (ConfigurableComponent.class.isAssignableFrom(extensionClass)) {
                final String extensionClassName = extensionClass.getCanonicalName();

                final Bundle bundle = ExtensionManager.getBundle(extensionClass.getClassLoader());
                if (bundle == null) {
                    logger.warn("No coordinate found for {}, skipping...", new Object[]{extensionClassName});
                    continue;
                }
                final BundleCoordinate coordinate = bundle.getBundleDetails().getCoordinate();

                final Class<? extends ConfigurableComponent> componentClass = extensionClass.asSubclass(ConfigurableComponent.class);
                try {
                    logger.error("Inspecting: " + componentClass);
                    // use temp components from ExtensionManager which should always be populated before doc generation
                    final String classType = componentClass.getCanonicalName();
                    final ConfigurableComponent component = ExtensionManager.getTempComponent(classType, coordinate);

                    for (PropertyDescriptor descriptor : component.getPropertyDescriptors()) {
                        logger.error("Found property {} -> {}", descriptor.getName(), descriptor.getDisplayName());
                    }

                } catch (Exception e) {
                    logger.warn("Inspecting: " + componentClass, e);
                }
            }
        }
    }

    protected Collection<Processor> getComponents(final String type, final String identifier, final BundleCoordinate bundleCoordinate, final Set<URL> additionalUrls) throws ProcessorInstantiationException {
        Set<Class> processorExtensions = ExtensionManager.getExtensions(Processor.class);
        for (final Class<?> extensionClass : processorExtensions) {
            if (ConfigurableComponent.class.isAssignableFrom(extensionClass)) {
                final String extensionClassName = extensionClass.getCanonicalName();

                final org.apache.nifi.bundle.Bundle bundle = ExtensionManager.getBundle(extensionClass.getClassLoader());
                if (bundle == null) {
                    logger.warn("No coordinate found for {}, skipping...", new Object[]{extensionClassName});
                    continue;
                }
                final BundleCoordinate coordinate = bundle.getBundleDetails().getCoordinate();

                final String path = coordinate.getGroup() + "/" + coordinate.getId() + "/" + coordinate.getVersion() + "/" + extensionClassName;

                final Class<? extends ConfigurableComponent> componentClass = extensionClass.asSubclass(ConfigurableComponent.class);
                try {
                    logger.error("Documenting: " + componentClass);


                    // use temp components from ExtensionManager which should always be populated before doc generation
                    final String classType = componentClass.getCanonicalName();
                    logger.error("Getting temp component: {}", classType);

                    final ConfigurableComponent component = ExtensionManager.getTempComponent(classType, coordinate);
                    logger.error("found component: {}", component);

                    final List<org.apache.nifi.components.PropertyDescriptor> properties = component.getPropertyDescriptors();

                    for (org.apache.nifi.components.PropertyDescriptor descriptor : properties) {
                        logger.error("Found descriptor: {} -> {}", descriptor.getName(), descriptor.getDisplayName());
                    }


                } catch (Exception e) {
                    logger.warn("Unable to document: " + componentClass, e);
                }
            }
        }

        final org.apache.nifi.bundle.Bundle processorBundle = ExtensionManager.getBundle(bundleCoordinate);
        if (processorBundle == null) {
            throw new ProcessorInstantiationException("Unable to find bundle for coordinate " + bundleCoordinate.getCoordinate());
        }


        final ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ClassLoader detectedClassLoaderForInstance = ExtensionManager.createInstanceClassLoader(type, identifier, processorBundle, additionalUrls);
            logger.error("Detected class laoder for instance={}", detectedClassLoaderForInstance == null);
            final Class<?> rawClass = Class.forName(type, true, detectedClassLoaderForInstance);
            logger.error("Raw class {}", rawClass.getName());
            Thread.currentThread().setContextClassLoader(detectedClassLoaderForInstance);

            final Class<? extends Processor> processorClass = rawClass.asSubclass(Processor.class);
            logger.error("processorClass class {}", processorClass.getName());
            final Processor processor = processorClass.newInstance();
            Set<Processor> processors = new HashSet<>();
            processors.add(processor);
            return processors;
        } catch (final Throwable t) {
            throw new ProcessorInstantiationException(type, t);
        } finally {
            if (ctxClassLoader != null) {
                Thread.currentThread().setContextClassLoader(ctxClassLoader);
            }
        }
    }
}
