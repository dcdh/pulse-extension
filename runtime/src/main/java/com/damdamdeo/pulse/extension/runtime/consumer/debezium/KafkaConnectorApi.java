package com.damdamdeo.pulse.extension.runtime.consumer.debezium;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/")
public interface KafkaConnectorApi {

    @GET
    @Path("/connectors")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getAllConnectors();

    @POST
    @Path("/connectors")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Void registerConnector(KafkaConnectorConfigurationDTO connectorConfiguration);

    @GET
    @Path("/connectors/{connectorName}/status")
    @Produces(MediaType.APPLICATION_JSON)
    KafkaConnectorStatusDTO connectorStatus(@PathParam("connectorName") String connectorName);
}
