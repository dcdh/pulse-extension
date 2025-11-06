package com.damdamdeo.pulse.extension.test.consumer.debezium;

import com.damdamdeo.pulse.extension.runtime.consumer.debezium.KafkaConnectorConfigurationDTO;
import com.damdamdeo.pulse.extension.runtime.consumer.debezium.KafkaConnectorConfigurationGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class KafkaConnectorConfigurationGeneratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.arc.exclude-types", "com.damdamdeo.pulse.extension.runtime.consumer.debezium.DebeziumConfigurator");

    @Inject
    KafkaConnectorConfigurationGenerator kafkaConnectorConfigurationGenerator;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @ConfigProperty(name = "pulse.debezium.connect.port")
    int pulseDebeziumConnectPort;

    @Test
    void shouldGenerateKafkaConnectorConfiguration() throws JsonProcessingException, JSONException {
        // Given

        // When
        final KafkaConnectorConfigurationDTO kafkaConnectorConfigurationDTO = kafkaConnectorConfigurationGenerator.generateConnectorConfiguration();

        // Then
        final String jsonConfiguration = objectMapper.writeValueAsString(kafkaConnectorConfigurationDTO);
        JSONAssert.assertEquals(
                // language=json
                """
                        {
                          "name": "todotaking_todo",
                          "config": {
                            "name": "todotaking_todo",
                            "schema": "todotaking_todo",
                            "database.hostname": "postgres",
                            "database.port": "5432",
                            "database.user": "quarkus",
                            "database.password": "password",
                            "database.dbname": "quarkus",
                            "key.converter": "org.apache.kafka.connect.json.JsonConverter",
                            "value.converter": "org.apache.kafka.connect.json.JsonConverter",
                            "key.converter.schemas.enable": "false",
                            "value.converter.schemas.enable": "false",
                            "topic.prefix": "pulse",
                            "plugin.name": "pgoutput",
                            "transforms": "unwrap,filterFields,partitioner",
                            "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
                            "transforms.unwrap.drop.tombstones": "false",
                            "transforms.unwrap.delete.handling.mode": "drop",
                            "transforms.unwrap.operation.header": "true",
                            "transforms.unwrap.add.headers": "source.version,source.connector,source.name,source.ts_ms,source.db,source.schema,source.table,source.txId,source.lsn",
                            "transforms.filterFields.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
                            "transforms.filterFields.include": "creation_date,event_type,event_payload,owned_by",
                            "transforms.partitioner.type": "io.debezium.transforms.partitions.PartitionRouting",
                            "transforms.partitioner.partition.payload.fields": "aggregate_root_type,aggregate_root_id",
                            "transforms.partitioner.partition.topic.num": 1,
                            "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                            "schema.include.list": "todotaking_todo",
                            "table.include.list": "todotaking_todo.t_event",
                            "tombstones.on.delete": "false",
                            "compression.type": "zstd",
                            "topic.creation.default.replication.factor": 1,
                            "topic.creation.default.partitions": 1,
                            "topic.creation.default.cleanup.policy": "compact",
                            "topic.creation.default.compression.type": "zstd"
                          }
                        }
                        """, jsonConfiguration, JSONCompareMode.STRICT);
    }

    @Test
    void shouldGeneratedKafkaConnectorConfigurationBeValid() throws JsonProcessingException {
// https://docs.confluent.io/platform/current/connect/references/restapi.html#put--connector-plugins-(string-name)-config-validate
        // Given
        final KafkaConnectorConfigurationDTO kafkaConnectorConfigurationDTO = kafkaConnectorConfigurationGenerator.generateConnectorConfiguration();
        final String jsonConfiguration = objectMapper.writeValueAsString(kafkaConnectorConfigurationDTO.config());// must takes config part

        // When && Then
        given()
                .baseUri("http://localhost:%d".formatted(pulseDebeziumConnectPort))
                .contentType(ContentType.JSON)
                .body(jsonConfiguration)
                .log().all()
                .when()
                .put("/connector-plugins/io.debezium.connector.postgresql.PostgresConnector/config/validate")
                .then()
                .log().all()
                .statusCode(200)
                .body("error_count", is(0));
    }
}
