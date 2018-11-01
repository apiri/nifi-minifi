package org.apache.nifi.device.registry.resource.c2.core.state;


import com.fasterxml.jackson.annotation.JsonProperty;

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


public class C2State {

    @JsonProperty("running")
    private boolean running;

    @JsonProperty("uptime")
    private long uptimeMilliseconds;

    public C2State() {
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getUptimeMilliseconds() {
        return uptimeMilliseconds;
    }

    public void setUptimeMilliseconds(long uptimeMilliseconds) {
        this.uptimeMilliseconds = uptimeMilliseconds;
    }
}
