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

import com.hortonworks.minifi.c2.model.AgentInfo;
import com.hortonworks.minifi.c2.model.AgentManifest;
import com.hortonworks.minifi.c2.model.AgentStatus;
import com.hortonworks.minifi.c2.model.BuildInfo;
import com.hortonworks.minifi.c2.model.C2Heartbeat;
import com.hortonworks.minifi.c2.model.DeviceInfo;
import com.hortonworks.minifi.c2.model.FlowInfo;
import com.hortonworks.minifi.c2.model.FlowStatus;
import com.hortonworks.minifi.c2.model.FlowUri;
import com.hortonworks.minifi.c2.model.extension.ComponentManifest;
import com.hortonworks.minifi.c2.model.extension.ControllerServiceDefinition;
import com.hortonworks.minifi.c2.model.extension.InputRequirement;
import com.hortonworks.minifi.c2.model.extension.ProcessorDefinition;
import com.hortonworks.minifi.c2.model.extension.PropertyAllowableValue;
import com.hortonworks.minifi.c2.model.extension.SchedulingDefaults;
import com.hortonworks.minifi.c2.model.extension.SchedulingStrategy;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.bundle.Bundle;
import org.apache.nifi.bundle.BundleCoordinate;
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.ConfigurableComponent;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.controller.exception.ProcessorInstantiationException;
import org.apache.nifi.minifi.nar.NarUnpacker;
import org.apache.nifi.minifi.nar.SystemBundle;
import org.apache.nifi.nar.ExtensionManager;
import org.apache.nifi.nar.NarClassLoaders;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.util.FileUtils;
import org.apache.nifi.util.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    protected C2Heartbeat generateHeartbeat() {

        final C2Heartbeat heartbeat = new C2Heartbeat();

        // Agent Info
        final AgentInfo agentInfo = generateAgentInfo();
        final DeviceInfo deviceInfo = generateDeviceInfo();
        final FlowInfo flowInfo = generateFlowInfo();

        heartbeat.setCreated(new Date().getTime());
        heartbeat.setIdentifier("minifi-java-id");
        // Populate heartbeat
        heartbeat.setAgentInfo(agentInfo);
        heartbeat.setDeviceInfo(deviceInfo);
        heartbeat.setFlowInfo(flowInfo);
        heartbeat.setCreated(new Date().getTime());
        heartbeat.setIdentifier("IDENTIFIER");

        return heartbeat;
    }

    private FlowInfo generateFlowInfo() {
        // Populate FlowInfo
        final FlowInfo flowInfo = new FlowInfo();
        flowInfo.setFlowId("flow identifer");
        FlowStatus flowStatus = new FlowStatus();
        flowStatus.setComponents(null);
        flowStatus.setQueues(null);
        flowInfo.setStatus(null);
        flowInfo.setComponents(null);
        flowInfo.setQueues(null);
        FlowUri flowUri = new FlowUri();
        flowUri.setBucketId("bucket-id");
        flowUri.setFlowId("flowuri-bucket-id");
        flowUri.setRegistryUrl("https://localhost:18080/nifi-registry");
        flowInfo.setVersionedFlowSnapshotURI(flowUri);
        return flowInfo;
    }

    private DeviceInfo generateDeviceInfo() {
        // Populate DeviceInfo
        final DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setIdentifier("DEVICEINFOIDENTIFIER");
        deviceInfo.setNetworkInfo(null);
        deviceInfo.setSystemInfo(null);
        return deviceInfo;
    }

    private AgentInfo generateAgentInfo() {
        final AgentInfo agentInfo = new AgentInfo();

        // Populate AgentInfo
        agentInfo.setAgentClass("devclass"); // TODO pull from conf file
        agentInfo.setIdentifier("AGENTINFOIDENTIFIER");

        final AgentStatus agentStatus = new AgentStatus(); // TODO implement
        agentStatus.setComponents(null);
        agentStatus.setRepositories(null);
        agentStatus.setUptime(System.currentTimeMillis());
        agentInfo.setStatus(agentStatus);

        agentInfo.setAgentManifest(generateAgentManifest());

        return agentInfo;
    }

    private AgentManifest generateAgentManifest() {
        final AgentManifest agentManifest = new AgentManifest();
        agentManifest.setAgentType("minifi-java");
        agentManifest.setVersion("1");
        agentManifest.setIdentifier("agent-manifest-id");
        BuildInfo buildInfo = new BuildInfo();
        buildInfo.setCompiler("JDK 8");
        buildInfo.setCompilerFlags("");
        buildInfo.setRevision("");
        buildInfo.setTargetArch("x86_64");
        buildInfo.setTimestamp(new Date().getTime());
        buildInfo.setVersion("1.8.0 u187");
        agentManifest.setBuildInfo(buildInfo);
        SchedulingDefaults schedulingDefaults = new SchedulingDefaults();
        schedulingDefaults.setDefaultMaxConcurrentTasks("1");
        schedulingDefaults.setDefaultRunDurationNanos(0);
        schedulingDefaults.setDefaultSchedulingPeriodMillis(0);
        schedulingDefaults.setDefaultSchedulingStrategy(SchedulingStrategy.TIMER_DRIVEN);
        schedulingDefaults.setPenalizationPeriodMillis(TimeUnit.SECONDS.convert(10, TimeUnit.MILLISECONDS));
        schedulingDefaults.setYieldDurationMillis(TimeUnit.SECONDS.convert(1, TimeUnit.MILLISECONDS));
        agentManifest.setSchedulingDefaults(schedulingDefaults);


        // Determine Bundles
        final List<com.hortonworks.minifi.c2.model.extension.Bundle> c2Bundles = new ArrayList<>();
        for (Bundle nifiBundle : ExtensionManager.getAllBundles()) {
            final BundleCoordinate bundleCoordinate = nifiBundle.getBundleDetails().getCoordinate();

            com.hortonworks.minifi.c2.model.extension.Bundle convertedBundle = new com.hortonworks.minifi.c2.model.extension.Bundle();

            convertedBundle.setArtifact(bundleCoordinate.getId());
            convertedBundle.setGroup(bundleCoordinate.getGroup());
            convertedBundle.setVersion(bundleCoordinate.getVersion());

            convertedBundle.setComponentManifest(getBundleManifest(nifiBundle));
            c2Bundles.add(convertedBundle);
        }

        // Determine components for the bundle
        // Determine manifest processors


//        bundleManifest.setProcessors();
//        bundleManifest.setControllerServices();
        // bundleManifest.setApis();
        // bundleManifest.setReportingTasks();  TODO: Currently not supported in DFM

        /** Component Manifest **/
//
//
//
//        // Populate Controller Services
//        final Set<Class> controllerService = ExtensionManager.getExtensions(ControllerService.class);
//        componentManifest.setControllerServices(null);
//        componentManifest.setApis(new ArrayList<>());
//        componentManifest.setReportingTasks(new ArrayList<>());

        agentManifest.setBundles(c2Bundles);
//        agentManifest.setComponentManifest(componentManifest);


        return agentManifest;
    }

    private ComponentManifest getBundleManifest(Bundle nifiBundle) {

        final BundleCoordinate bundleCoordinate = nifiBundle.getBundleDetails().getCoordinate();

        final Set<Class> procExtensions = ExtensionManager.getExtensions(Processor.class);
        final Set<Class> csExtensions = ExtensionManager.getExtensions(ControllerService.class);

        // Determine Processors
        final List<ProcessorDefinition> processorDefinitions = new ArrayList<>();
        final List<ControllerServiceDefinition> csDefinitions = new ArrayList<>();

        final Map<Class, Bundle> processorClassBundles = new HashMap<>();
        for (final Class cls : procExtensions) {
            processorClassBundles.put(cls, ExtensionManager.getBundle(cls.getClassLoader()));
        }

        final Map<Class, Bundle> csClassBundles = new HashMap<>();
        for (final Class cls : csExtensions) {
            csClassBundles.put(cls, ExtensionManager.getBundle(cls.getClassLoader()));
        }


        for (Class cls : procExtensions) {
            if (!processorClassBundles.get(cls).equals(nifiBundle)) {
                continue;
            }
            final ProcessorDefinition procDef = new ProcessorDefinition();
            procDef.setType(cls.getName());
            procDef.setTypeDescription(getCapabilityDescription(cls));
            procDef.setPropertyDescriptors(getPropertyDescriptors(cls));
            procDef.setInputRequirement(InputRequirement.INPUT_ALLOWED);
            procDef.setSupportedRelationships(getSupportedRelationships(cls));
            procDef.setSupportsDynamicProperties(true);
            procDef.setSupportsDynamicRelationships(true);
            procDef.setArtifact(bundleCoordinate.getId());
            procDef.setVersion(bundleCoordinate.getVersion());
            procDef.setGroup(bundleCoordinate.getGroup());
            processorDefinitions.add(procDef);
        }

        // Determine Controller Services
        for (Class cls : csExtensions) {
//                logger.error("Looking up bundle for ControllerService class {}", cls);
            if (!csClassBundles.get(cls).equals(nifiBundle)) {
                continue;
            }
            final ControllerServiceDefinition csDef = new ControllerServiceDefinition();
            csDef.setType(cls.getName());
            csDef.setTypeDescription(getCapabilityDescription(cls));
            csDef.setPropertyDescriptors(getPropertyDescriptors(cls));
            csDef.setSupportsDynamicProperties(true);
            csDef.setArtifact(bundleCoordinate.getId());
            csDef.setVersion(bundleCoordinate.getVersion());
            csDef.setGroup(bundleCoordinate.getGroup());
            csDefinitions.add(csDef);
        }

        ComponentManifest bundleComponentManifest = new ComponentManifest();
        bundleComponentManifest.setControllerServices(csDefinitions);
        bundleComponentManifest.setProcessors(processorDefinitions);

        return bundleComponentManifest;
    }

    private List<com.hortonworks.minifi.c2.model.extension.Relationship> getSupportedRelationships(Class extClass) {
//        logger.error("GENERATING PROPERTY DESCRIPTORS for {}", extClass.getName());

        final List<com.hortonworks.minifi.c2.model.extension.Relationship> relationships = new ArrayList<>();

        if (ConfigurableComponent.class.isAssignableFrom(extClass)) {
            final String extensionClassName = extClass.getCanonicalName();

            final Bundle bundle = ExtensionManager.getBundle(extClass.getClassLoader());
            if (bundle == null) {
                logger.warn("No coordinate found for {}, skipping...", new Object[]{extensionClassName});
            }
            final BundleCoordinate coordinate = bundle.getBundleDetails().getCoordinate();

            final Class<? extends ConfigurableComponent> componentClass = extClass.asSubclass(ConfigurableComponent.class);
            try {
                // use temp components from ExtensionManager which should always be populated before doc generation
                final String classType = componentClass.getCanonicalName();
                final ConfigurableComponent component = ExtensionManager.getTempComponent(classType, coordinate);

                if (component instanceof Processor) {
//                    logger.error("GETTING RELATIONSHIPS.....");
                    final Processor connectable = (Processor) component;
                    for (Relationship rel : connectable.getRelationships()) {
                        com.hortonworks.minifi.c2.model.extension.Relationship c2Rel = new com.hortonworks.minifi.c2.model.extension.Relationship();
                        c2Rel.setName(rel.getName());
                        c2Rel.setDescription(rel.getDescription());
                        relationships.add(c2Rel);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error generating relationships...: " + componentClass, e);
            }
        }
        return relationships;
    }

    private String findType(Class extClass) {
//        logger.error("GENERATING PROPERTY DESCRIPTORS for {}", extClass.getName());

        final List<com.hortonworks.minifi.c2.model.extension.Relationship> relationships = new ArrayList<>();

        if (ConfigurableComponent.class.isAssignableFrom(extClass)) {
            final String extensionClassName = extClass.getCanonicalName();

            final Bundle bundle = ExtensionManager.getBundle(extClass.getClassLoader());
            if (bundle == null) {
                logger.warn("No coordinate found for {}, skipping...", new Object[]{extensionClassName});
            }
            final BundleCoordinate coordinate = bundle.getBundleDetails().getCoordinate();

            final Class<? extends ConfigurableComponent> componentClass = extClass.asSubclass(ConfigurableComponent.class);
            try {
                // use temp components from ExtensionManager which should always be populated before doc generation
                final String classType = componentClass.getCanonicalName();
                final ConfigurableComponent component = ExtensionManager.getTempComponent(classType, coordinate);

                if (component instanceof ControllerService) {
                    final ControllerService connectable = (ControllerService) component;
                }
            } catch (Exception e) {
                logger.warn("Error generating relationships...: " + componentClass, e);
            }
        }
        return "";
    }


    private LinkedHashMap<String, com.hortonworks.minifi.c2.model.extension.PropertyDescriptor> getPropertyDescriptors
            (Class extClass) {

        final LinkedHashMap<String, com.hortonworks.minifi.c2.model.extension.PropertyDescriptor> c2PropDescriptors = new LinkedHashMap<>();


        if (ConfigurableComponent.class.isAssignableFrom(extClass)) {
            final String extensionClassName = extClass.getCanonicalName();

            final Bundle bundle = ExtensionManager.getBundle(extClass.getClassLoader());
            if (bundle == null) {
                logger.warn("No coordinate found for {}, skipping...", new Object[]{extensionClassName});
            }
            final BundleCoordinate coordinate = bundle.getBundleDetails().getCoordinate();

            final Class<? extends ConfigurableComponent> componentClass = extClass.asSubclass(ConfigurableComponent.class);
            try {
                // use temp components from ExtensionManager which should always be populated before doc generation
                final String classType = componentClass.getCanonicalName();
                final ConfigurableComponent component = ExtensionManager.getTempComponent(classType, coordinate);

                for (PropertyDescriptor descriptor : component.getPropertyDescriptors()) {
                    final com.hortonworks.minifi.c2.model.extension.PropertyDescriptor c2Prop = new com.hortonworks.minifi.c2.model.extension.PropertyDescriptor();
                    final List<AllowableValue> allowableValues = descriptor.getAllowableValues();
                    if (allowableValues != null && !allowableValues.isEmpty()) {
                        c2Prop.setAllowableValues(convert(allowableValues));
                    }
                    c2Prop.setDefaultValue(descriptor.getDefaultValue());
                    c2Prop.setDescription("description");
                    c2Prop.setDisplayName(descriptor.getDisplayName());
                    c2Prop.setDynamic(descriptor.isDynamic());
                    c2Prop.setName(descriptor.getName());

                    c2PropDescriptors.put(descriptor.getName(), c2Prop);
                }

            } catch (Exception e) {
                logger.warn("Error generating property descriptors...: " + componentClass, e);
            }
        }


        return c2PropDescriptors;
    }

    private List<PropertyAllowableValue> convert(List<AllowableValue> allowableValues) {

        List<PropertyAllowableValue> propertyAllowableValues = new ArrayList<>();

        for (AllowableValue allowValue : allowableValues) {
            PropertyAllowableValue pav = new PropertyAllowableValue();
            pav.setDescription(allowValue.getDescription());
            pav.setDisplayName(allowValue.getDisplayName());
            pav.setValue(allowValue.getValue());
            propertyAllowableValues.add(pav);
        }

        return propertyAllowableValues;
    }

    protected Collection<Processor> getComponents(final String type, final String identifier,
                                                  final BundleCoordinate bundleCoordinate, final Set<URL> additionalUrls) throws ProcessorInstantiationException {
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

    private final static Comparator<Class> CLASS_NAME_COMPARATOR = (class1, class2) -> Collator.getInstance(Locale.US).compare(class1.getSimpleName(), class2.getSimpleName());


//    private Processor instantiateProcessor(final String type, final String identifier, final BundleCoordinate bundleCoordinate, final Set<URL> additionalUrls) throws ProcessorInstantiationException {
//
//        Set<Class> processorExtensions = ExtensionManager.getExtensions(Processor.class);
//        for (final Class<?> extensionClass : processorExtensions) {
//            if (ConfigurableComponent.class.isAssignableFrom(extensionClass)) {
//                final String extensionClassName = extensionClass.getCanonicalName();
//
//                final org.apache.nifi.bundle.Bundle bundle = ExtensionManager.getBundle(extensionClass.getClassLoader());
//                if (bundle == null) {
//                    logger.warn("No coordinate found for {}, skipping...", new Object[]{extensionClassName});
//                    continue;
//                }
//                final BundleCoordinate coordinate = bundle.getBundleDetails().getCoordinate();
//
//                final String path = coordinate.getGroup() + "/" + coordinate.getId() + "/" + coordinate.getVersion() + "/" + extensionClassName;
//
//                final Class<? extends ConfigurableComponent> componentClass = extensionClass.asSubclass(ConfigurableComponent.class);
//                try {
//                    logger.error("Documenting: " + componentClass);
//
//
//                    // use temp components from ExtensionManager which should always be populated before doc generation
//                    final String classType = componentClass.getCanonicalName();
//                    logger.error("Getting temp component: {}", classType);
//
//                    final ConfigurableComponent component = ExtensionManager.getTempComponent(classType, bundleCoordinate);
//                    logger.error("found component: {}", component);
//
//                    final List<org.apache.nifi.components.PropertyDescriptor> properties = component.getPropertyDescriptors();
//
//                    for (org.apache.nifi.components.PropertyDescriptor descriptor : properties) {
//                        logger.error("Found descriptor: {} -> {}", descriptor.getName(), descriptor.getDisplayName());
//                    }
//
//
//                } catch (Exception e) {
//                    logger.warn("Unable to document: " + componentClass, e);
//                }
//            }
//        }
//
//        final org.apache.nifi.bundle.Bundle processorBundle = ExtensionManager.getBundle(bundleCoordinate);
//        if (processorBundle == null) {
//            throw new ProcessorInstantiationException("Unable to find bundle for coordinate " + bundleCoordinate.getCoordinate());
//        }
//
//
//        final ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
//        try {
//            final ClassLoader detectedClassLoaderForInstance = ExtensionManager.createInstanceClassLoader(type, identifier, processorBundle, additionalUrls);
//            logger.error("Detected class laoder for instance={}", detectedClassLoaderForInstance == null);
//            final Class<?> rawClass = Class.forName(type, true, detectedClassLoaderForInstance);
//            logger.error("Raw class {}", rawClass.getName());
//            Thread.currentThread().setContextClassLoader(detectedClassLoaderForInstance);
//
//            final Class<? extends Processor> processorClass = rawClass.asSubclass(Processor.class);
//            logger.error("processorClass class {}", processorClass.getName());
//            final Processor processor = processorClass.newInstance();
//            return processor;
//        } catch (final Throwable t) {
//            throw new ProcessorInstantiationException(type, t);
//        } finally {
//            if (ctxClassLoader != null) {
//                Thread.currentThread().setContextClassLoader(ctxClassLoader);
//            }
//        }
//    }

    /**
     * Gets the capability description from the specified class.
     */
    private String getCapabilityDescription(final Class<?> cls) {
        final CapabilityDescription capabilityDesc = cls.getAnnotation(CapabilityDescription.class);
        return capabilityDesc == null ? null : capabilityDesc.value();
    }
}
