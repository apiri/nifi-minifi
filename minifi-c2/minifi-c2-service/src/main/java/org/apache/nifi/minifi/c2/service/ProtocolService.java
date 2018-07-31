package org.apache.nifi.minifi.c2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;
import org.apache.nifi.minifi.c2.api.security.authorization.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

@Path("/protocol-bak")
@Api(
        value = "/protocol",
        description = "Provides protocol endpoints for MiNiFi instances to receive operations"
)
public class ProtocolService {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolService.class);
    private final Authorizer authorizer;
    private final ObjectMapper objectMapper;

    public ProtocolService(Authorizer authorizer) {
        this.authorizer = authorizer;
        this.objectMapper = new ObjectMapper();
    }

//    @POST
//    @Path("/heartbeat")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response heartbeat(@Context HttpServletRequest request, @Context UriInfo uriInfo) {
//        try {
//            authorizer.authorize(SecurityContextHolder.getContext().getAuthentication(), uriInfo);
//        } catch (AuthorizationException e) {
//            logger.warn(HttpRequestUtil.getClientString(request) + " not authorized to access " + uriInfo, e);
//            return Response.status(403).build();
//        }
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        List<String> contentTypes;
//        try {
//            contentTypes = configurationProviderInfo.get().getContentTypes();
//        } catch (ConfigurationProviderException e) {
//            logger.warn("Unable to initialize content type information.", e);
//            return Response.status(500).build();
//        }
//        try {
//            objectMapper.writerWithDefaultPrettyPrinter().writeValue(byteArrayOutputStream, contentTypes);
//        } catch (IOException e) {
//            logger.warn("Unable to write configuration providers to output stream.", e);
//            return Response.status(500).build();
//        }
//        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(byteArrayOutputStream.toByteArray()).build();
//    }
//
//
//    @POST
//    @Path("/acknowledge")
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response acknowledge(@Context HttpServletRequest request, @Context UriInfo uriInfo) {
//        try {
//            authorizer.authorize(SecurityContextHolder.getContext().getAuthentication(), uriInfo);
//        } catch (AuthorizationException e) {
//            logger.warn(HttpRequestUtil.getClientString(request) + " not authorized to access " + uriInfo, e);
//            return Response.status(403).build();
//        }
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        List<String> contentTypes;
//        try {
//            contentTypes = configurationProviderInfo.get().getContentTypes();
//        } catch (ConfigurationProviderException e) {
//            logger.warn("Unable to initialize content type information.", e);
//            return Response.status(500).build();
//        }
//        try {
//            objectMapper.writerWithDefaultPrettyPrinter().writeValue(byteArrayOutputStream, contentTypes);
//        } catch (IOException e) {
//            logger.warn("Unable to write configuration providers to output stream.", e);
//            return Response.status(500).build();
//        }
//        return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(byteArrayOutputStream.toByteArray()).build();
//    }
}
