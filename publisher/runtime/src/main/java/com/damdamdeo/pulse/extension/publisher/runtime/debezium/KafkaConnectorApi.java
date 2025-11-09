package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/")
public interface KafkaConnectorApi {

    // https://docs.confluent.io/platform/current/connect/references/restapi.html#get--connectors
    @GET
    @Path("/connectors")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getAllConnectors();

    // https://docs.confluent.io/platform/current/connect/references/restapi.html#post--connectors
    @POST
    @Path("/connectors")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    CreatedConnectorResponseDTO registerConnector(KafkaConnectorConfigurationDTO connectorConfiguration);

    // https://docs.confluent.io/platform/current/connect/references/restapi.html#get--connectors-(string-name)-status
    @GET
    @Path("/connectors/{connectorName}/status")
    @Produces(MediaType.APPLICATION_JSON)
    KafkaConnectorStatusDTO connectorStatus(@PathParam("connectorName") String connectorName);
}
