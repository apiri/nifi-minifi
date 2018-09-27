package org.apache.nifi.device.registry.resource.operations;

import com.codahale.metrics.annotation.Timed;
import org.apache.nifi.controller.status.ConnectionStatus;
import org.apache.nifi.device.registry.GarconConfiguration;
import org.apache.nifi.device.registry.dto.ConnectionsHUD;
import org.apache.nifi.device.registry.service.ConnectionService;
import org.apache.nifi.device.registry.service.impl.ConnectionServiceImpl;
import org.apache.nifi.minifi.c2.dao.ConnectionDAO;
import org.apache.nifi.minifi.c2.dao.impl.ConnectionDAOImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
 * Created on 3/30/17.
 */

@Path("/api/v1/connection")
@Produces(MediaType.APPLICATION_JSON)
public class ConnectionsResource {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionsResource.class);

    private GarconConfiguration configuration;
    private ConnectionService connectionService = null;
    private ConnectionDAO conDao = null;

    public ConnectionsResource(GarconConfiguration conf) {
        this.configuration = conf;
        this.connectionService = ConnectionServiceImpl.getInstance();
        this.conDao = ConnectionDAOImpl.getInstance();
    }

    @GET
    @Timed
    public Response getRegisteredDevices() {
        logger.info("Retrieving pressured connections");
        return Response.ok(conDao.getLatestPressuredConnectionForDevice(null)).build();
    }

    @GET
    @Timed
    @Path("/{connectionId}")
    public Response getDetailedConnectionStatusForConnectionById(@PathParam("connectionId") String connectionId) {
        logger.info("Retrieving detailed information for backpressured connection with ID: {}", new Object[]{connectionId});
        return Response.ok(conDao.getPressuredConnectionDetails("1", connectionId)).build();
    }

    @GET
    @Timed
    @Path("/hud")
    public Response getConnectionsHUD() {
        logger.info("Retrieving registry connection HUD");
        ConnectionsHUD hud = connectionService.generateConnectionsHUD(null);
        return Response.ok(hud).build();
    }

    @POST
    @Timed
    @Path("/pressured")
    public Response addPressuredConnections(List<ConnectionStatus> pressuredConnections) {
        if (logger.isDebugEnabled()) {
            logger.debug("Received PressuredConnections: " + pressuredConnections);
        }
        conDao.insertPressuredConnectionForDevice(pressuredConnections, "1");
        return Response.ok().build();
    }
}
