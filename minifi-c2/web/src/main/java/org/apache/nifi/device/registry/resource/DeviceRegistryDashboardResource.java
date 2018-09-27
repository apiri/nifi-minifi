package org.apache.nifi.device.registry.resource;

import com.codahale.metrics.annotation.Timed;
import org.apache.nifi.device.registry.GarconConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
 * Created on 4/8/17.
 */

@Component
@Path("/api/v1/device/registry/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DeviceRegistryDashboardResource {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryDashboardResource.class);

    private GarconConfiguration configuration;

    public DeviceRegistryDashboardResource(GarconConfiguration conf) {
        this.configuration = conf;
    }

    @GET
    @Timed
    public Response getHUD() {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving Device Registry Dashboard DTO");
        }

        return Response.ok().build();
    }
}
