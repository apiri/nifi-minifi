package org.apache.nifi.device.registry.resource.c2.core;

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
 * <p>
 * Created on 7/7/17.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.nifi.device.registry.resource.c2.core.device.DeviceInfo;
import org.apache.nifi.device.registry.resource.c2.core.metrics.C2Metrics;
import org.apache.nifi.device.registry.resource.c2.core.state.C2State;

import java.util.Map;

/**
 * Payload received from the MiNiFi-CPP C2Agent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class C2Payload {

    @JsonProperty("DeviceInfo")
    private DeviceInfo deviceInfo;

    @JsonProperty("metrics")
    private C2Metrics metrics;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("state")
    private C2State state;

    @JsonProperty("Components")
    private Map<String,String> components;

    public C2Payload() {
    }

    @JsonProperty("DeviceInfo")
    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    @JsonProperty("DeviceInfo")
    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    @JsonProperty("operation")
    public String getOperation() {
        return operation;
    }

    @JsonProperty("operation")
    public void setOperation(String operation) {
        this.operation = operation;
    }

    @JsonProperty("metrics")
    public C2Metrics getMetrics() {
        return metrics;
    }

    @JsonProperty("metrics")
    public void setMetrics(C2Metrics metrics) {
        this.metrics = metrics;
    }

    @JsonProperty("state")
    public C2State getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(C2State state) {
        this.state = state;
    }

    public Map<String,String> getComponents(){ return this.components; }

    public void setComponents(Map<String,String> components){ this.components = components; }
}
