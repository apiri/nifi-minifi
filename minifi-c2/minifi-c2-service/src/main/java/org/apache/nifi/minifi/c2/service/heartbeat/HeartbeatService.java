package org.apache.nifi.minifi.c2.service.heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;
import org.apache.nifi.minifi.c2.api.HeartbeatConsumer;
import org.apache.nifi.minifi.c2.api.Operation;
import org.apache.nifi.minifi.c2.api.OperationRequest;
import org.apache.nifi.minifi.c2.api.OperationResponse;
import org.apache.nifi.minifi.c2.api.security.authorization.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/heartbeat")
@Api(
        value = "/heartbeat",
        description = "Provides heartbeat service with a return payload of options to "

)
public class HeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);
    private final Authorizer authorizer;
    private final ObjectMapper objectMapper;

    public HeartbeatService(List<HeartbeatConsumer> heartbeatConsumers, Authorizer authorizer) {
        this.authorizer = authorizer;
        this.objectMapper = new ObjectMapper();
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consumeHeartbeat(@Context HttpServletRequest request, @Context UriInfo uriInfo) {

        // Authorize
//        try {
//            authorizer.authorize(SecurityContextHolder.getContext().getAuthentication(), uriInfo);
//        } catch (AuthorizationException e) {
//            logger.warn(HttpRequestUtil.getClientString(request) + " not authorized to access " + uriInfo, e);
//            return Response.status(403).build();
//        }

        // Comprehend/validate POST request
        // Update/create device registry information
        // Lookup of available updates
        // Provide response
        OperationResponse operationResponse = new OperationResponse(Operation.HEARTBEAT);
        OperationRequest operationRequest = new OperationRequest(Operation.CLEAR, "connection");
        operationRequest.addContent("TransferFilesToRPG");
        operationResponse.addRequest(operationRequest);
        try {
            return Response.status(Response.Status.OK).entity(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(operationResponse)).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
