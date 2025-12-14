package com.damdamdeo.pulse.extension.publisher.deployment.debezium;

import com.damdamdeo.pulse.extension.publisher.runtime.debezium.DebeziumConfigurator;
import com.damdamdeo.pulse.extension.publisher.runtime.debezium.KafkaConnectorConfigurationDTO;
import com.damdamdeo.pulse.extension.publisher.runtime.debezium.KafkaConnectorConfigurationGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.regex.Matcher;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

class KafkaConnectorConfigurationGeneratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.arc.exclude-types", DebeziumConfigurator.class.getName());

    @Inject
    KafkaConnectorConfigurationGenerator kafkaConnectorConfigurationGenerator;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @ConfigProperty(name = "pulse.debezium.connect.port")
    int pulseDebeziumConnectPort;

    @Test
    void shouldExtractPostgresUrlParameters() {
        // Given
        final String givenJdbcPostgresUrl = "jdbc:postgresql://localhost:34879/quarkus";

        // When
        final Matcher matcher = KafkaConnectorConfigurationGenerator.JDBC_POSTGRES_PATTERN.matcher(givenJdbcPostgresUrl);

        // Then
        assertAll(
                () -> assertThat(matcher.matches()).isTrue(),
                () -> assertThat(matcher.group("host")).isEqualTo("localhost"),
                () -> assertThat(matcher.group("port")).isEqualTo("34879"),
                () -> assertThat(matcher.group("database")).isEqualTo("quarkus")
        );
    }

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
                            "database.password": "quarkus",
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
                            "transforms.filterFields.include": "creation_date,event_type,event_payload,owned_by,belongs_to",
                            "transforms.partitioner.type": "io.debezium.transforms.partitions.PartitionRouting",
                            "transforms.partitioner.partition.payload.fields": "belongs_to",
                            "transforms.partitioner.partition.topic.num": 1,
                            "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                            "schema.include.list": "todotaking_todo",
                            "table.include.list": "todotaking_todo.t_event",
                            "tombstones.on.delete": "false",
                            "compression.type": "zstd",
                            "topic.creation.default.replication.factor": 1,
                            "topic.creation.default.partitions": 1,
                            "topic.creation.default.cleanup.policy": "compact",
                            "topic.creation.default.compression.type": "zstd",
                            "publication.name": "todotaking_todo_publication",
                            "publication.autocreate.mode": "all_tables",
                            "slot.name": "todotaking_todo_slot",
                            "slot.drop.on.stop": "false"
                          }
                        }
                        """, jsonConfiguration, JSONCompareMode.STRICT);
        /*
{
  "name": "todotaking_todo_notification_mirror",
  "config": {
    "connector.class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
    "source.cluster.alias": "primary",
    "target.cluster.alias": "primary",
    "source.cluster.bootstrap.servers": "kafka:9092",
    "target.cluster.bootstrap.servers": "kafka:9092",
    "topics": "pulse.todotaking_todo.t_event",
    "topic.creation.default.replication.factor": 1,
    "topic.creation.default.partitions": 1,
    "topic.creation.default.cleanup.policy": "delete",
    "topic.creation.default.retention.ms": 30000,
    "topic.creation.default.compression.type": "zstd",
    "replication.factor": 1,
    "sync.topic.configs.enabled": "false",
    "sync.topic.acls.enabled": "false",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "pulse\\.todotaking_todo\\.t_event",
    "transforms.route.replacement": "pulse.notification.todotaking_todo.t_event"
  }
}
*/
        la notification devrait etre lié à un aggregate id + root + from application
            l'idempotency est compliqué parce que je peux avoir plusieurs application qui ecoute depuis le debut ...
            je devrait considéré que je peux publier si la version == la version courante dans  ... mais si entre temps il y a une nouvelle version bah je ne vois pas celle - ci...
        fuck off c'est compliqué ...


            alors je peux faire du 2h et comme je prends le latest je n'ai pas de rejoue !
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
