package com.damdamdeo.pulse.extension.publisher.deployment.debezium;

import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PartitionTest {

    private static final String OWNED_BY_A = "OwnedByA";
    private static final String OWNED_BY_B = "OwnedByB";

    private static final String EVENT_TOPIC = "pulse.todotaking_todo.event";
    private static final String AGGREGATE_ROOT_TOPIC = "pulse.todotaking_todo.aggregate_root";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("pulse.debezium.topic-creation.default-partitions", "2");

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OpenPGPEncryptionService openPGPEncryptionService;

    // https://debezium.io/documentation/reference/stable/transformations/partition-routing.html#partition-routing-partition-hash-function
    // https://github.com/debezium/debezium/blob/main/debezium-core/src/main/java/io/debezium/transforms/partitions/PartitionRouting.java#L250
    @Test
    void shouldOwnedByBBeAssignedToPartitionZERO() {
        // Given

        // When
        final int i = computePartition(2, List.of(OWNED_BY_B));

        // Then
        assertThat(i).isEqualTo(0);
    }

    @Test
    void shouldOwnedByABeAssignedToPartitionOne() {
        // Given

        // When
        final int i = computePartition(2, List.of(OWNED_BY_A));

        // Then
        assertThat(i).isEqualTo(1);
    }

    // https://github.com/debezium/debezium/blob/8f9481a69cc0418e7614093f834bc9625cd2d390/debezium-core/src/main/java/io/debezium/transforms/partitions/PartitionRouting.java#L242
    protected int computePartition(final Integer partitionNumber, final List<Object> values) {
        // use Object::hashCode
        int totalHashCode = values.stream().map(Object::hashCode).reduce(0, Integer::sum);
        // hashCode can be negative due to overflow. Since Math.abs(Integer.MIN_INT) will still return a negative number
        // we use bitwise operation to remove the sign
        int normalizedHash = totalHashCode & Integer.MAX_VALUE;
        if (normalizedHash == Integer.MAX_VALUE) {
            normalizedHash = 0;
        }
        return normalizedHash % partitionNumber;
    }

    // from DebeziumPublisherTest
    @Test
    @Order(1)
    void shouldHaveInitializedTwoPartitions() throws InterruptedException {
        // Given / Must push messages to create the topic
        Integer index = 0;
        for (final String ownedBy : List.of(OWNED_BY_A, OWNED_BY_B)) {
            // language=sql
            final String tEventSql = """
                    INSERT INTO event (aggregate_root_id, aggregate_root_type, version, creation_date, event_type, event_payload, owned_by, belongs_to, executed_by) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            // language=sql
            final String tAggregateRootSql = """
                        INSERT INTO aggregate_root (aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, belongs_to)
                        VALUES (?, ?, ?, ?, ?, ?)
                    """;
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement tEventPS = connection.prepareStatement(tEventSql);
                 final PreparedStatement tAggregateRootPS = connection.prepareStatement(tAggregateRootSql)) {

                tEventPS.setString(1, index.toString());
                tEventPS.setString(2, "Todo");
                tEventPS.setLong(3, 0);
                tEventPS.setObject(4, Timestamp.from(Instant.ofEpochMilli(1_000_000_000L)));
                tEventPS.setString(5, "NewTodoCreated");
                tEventPS.setBytes(6, openPGPEncryptionService.encrypt(
                        // language=json
                        """
                                {
                                  "description": "lorem ipsum",
                                  "status": "DONE",
                                  "important": false
                                }
                                """.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload());
                tEventPS.setString(7, ownedBy);
                tEventPS.setString(8, ownedBy);
                tEventPS.setString(9, "EU:encodedbob");
                tEventPS.executeUpdate();

                tAggregateRootPS.setString(1, index.toString());
                tAggregateRootPS.setString(2, Todo.class.getSimpleName());
                tAggregateRootPS.setLong(3, 1);
                tAggregateRootPS.setBytes(4, openPGPEncryptionService.encrypt(
                        // language=json
                        """
                                {
                                  "id": "Damien/0",
                                  "description": "lorem ipsum",
                                  "status": "DONE",
                                  "important": false
                                }
                                """.getBytes(StandardCharsets.UTF_8), PassphraseSample.PASSPHRASE).payload());
                tAggregateRootPS.setString(5, ownedBy);
                tAggregateRootPS.setString(6, ownedBy);
                tAggregateRootPS.executeUpdate();

                index++;
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }

        // When
        final List<String> expectedTopics = List.of(EVENT_TOPIC, AGGREGATE_ROOT_TOPIC);

        await().atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    try (final AdminClient adminClient = AdminClient.create(Map.of(
                            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class)))) {
                        final List<String> topicsPresents = adminClient.listTopics().listings().get()
                                .stream().map(TopicListing::name)
                                .toList();
                        return topicsPresents.containsAll(expectedTopics);
                    }
                });

        // Then
        try (final AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class)))) {

            final DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(expectedTopics);
            for (final Map.Entry<String, KafkaFuture<TopicDescription>> entry : describeTopicsResult.topicNameValues().entrySet()) {
                final KafkaFuture<TopicDescription> topicDescriptionKafkaFuture = entry.getValue();
                final TopicDescription topicDescription = topicDescriptionKafkaFuture.get();
                System.out.printf(
                        "Topic=%s Partition=%s%n",
                        topicDescription.name(),
                        topicDescription.partitions().stream().map(TopicPartitionInfo::toString).collect(Collectors.joining(",")));
                assertThat(topicDescription.partitions()).hasSize(2);
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(2)
    void shouldSplitMessagesByPartition() {
        // Given
        final List<TopicPartition> partitions = List.of(
                new TopicPartition(EVENT_TOPIC, 0),
                new TopicPartition(EVENT_TOPIC, 1),
                new TopicPartition(AGGREGATE_ROOT_TOPIC, 0),
                new TopicPartition(AGGREGATE_ROOT_TOPIC, 1));

        final Map<TopicPartition, TopicInfo> topicInfos = partitions.stream()
                .collect(Collectors.toMap(tp -> tp, TopicInfo::new));

        final Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "offset-inspector");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.assign(partitions);

            final Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);
            final Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            // Seek au début
            for (final TopicPartition tp : partitions) {
                consumer.seek(tp, beginningOffsets.get(tp));
            }

            boolean done;
            do {
                done = true;
                final var records = consumer.poll(java.time.Duration.ofMillis(500));
                records.forEach(record -> {
                    final TopicPartition tp =
                            new TopicPartition(record.topic(), record.partition());
                    topicInfos.get(tp).add(record);
                });
                // Continue tant qu'il reste des messages
                for (final TopicPartition tp : partitions) {
                    if (consumer.position(tp) < endOffsets.get(tp)) {
                        done = false;
                        break;
                    }
                }
            } while (!done);
        }

        assertAll(
                // Then — assertions métier
                () -> assertThat(topicInfos.values()).allSatisfy(topicInfo -> assertThat(topicInfo.records()).hasSize(1)),
                // Assertions exactes
                () -> assertThat(topicInfos.get(new TopicPartition(EVENT_TOPIC, 0)).records()).hasSize(1),
                () -> assertThat(topicInfos.get(new TopicPartition(EVENT_TOPIC, 0)).getFirstRecordOwnedBy(objectMapper)).isEqualTo("OwnedByB"),
                () -> assertThat(topicInfos.get(new TopicPartition(EVENT_TOPIC, 1)).records()).hasSize(1),
                () -> assertThat(topicInfos.get(new TopicPartition(EVENT_TOPIC, 1)).getFirstRecordOwnedBy(objectMapper)).isEqualTo("OwnedByA"),
                () -> assertThat(topicInfos.get(new TopicPartition(AGGREGATE_ROOT_TOPIC, 0)).records()).hasSize(1),
                () -> assertThat(topicInfos.get(new TopicPartition(AGGREGATE_ROOT_TOPIC, 0)).getFirstRecordOwnedBy(objectMapper)).isEqualTo("OwnedByB"),
                () -> assertThat(topicInfos.get(new TopicPartition(AGGREGATE_ROOT_TOPIC, 1)).records()).hasSize(1),
                () -> assertThat(topicInfos.get(new TopicPartition(AGGREGATE_ROOT_TOPIC, 1)).getFirstRecordOwnedBy(objectMapper)).isEqualTo("OwnedByA")
        );

        // Debug lisible
        topicInfos.values().forEach(ti ->
                ti.records().forEach(r ->
                        System.out.printf(
                                "Consumed %s-%d offset=%d key=%s value=%s%n",
                                r.topic(), r.partition(), r.offset(), r.key(), r.value())
                ));
    }

    public static final class TopicInfo {

        private final TopicPartition topicPartition;
        private final List<ConsumerRecord<String, String>> records = new ArrayList<>();

        public TopicInfo(final TopicPartition topicPartition) {
            this.topicPartition = topicPartition;
        }

        public TopicPartition topicPartition() {
            return topicPartition;
        }

        public List<ConsumerRecord<String, String>> records() {
            return records;
        }

        public String getFirstRecordOwnedBy(final ObjectMapper objectMapper) {
            try {
                final ConsumerRecord<String, String> first = records.getFirst();
                final JsonNode jsonNode = objectMapper.readTree(first.value());
                return jsonNode.get("owned_by").asText();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public void add(final ConsumerRecord<String, String> record) {
            records.add(record);
        }
    }
}
