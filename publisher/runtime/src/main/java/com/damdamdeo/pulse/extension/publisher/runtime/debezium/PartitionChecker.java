package com.damdamdeo.pulse.extension.publisher.runtime.debezium;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Singleton
@Unremovable
public class PartitionChecker {

    private final DebeziumConfiguration debeziumConfiguration;
    private final String bootstrapServers;
    private final String quarkusApplicationName;

    public PartitionChecker(final DebeziumConfiguration debeziumConfiguration,
                            @ConfigProperty(name = "kafka.bootstrap.servers") final String bootstrapServers,
                            @ConfigProperty(name = "quarkus.application.name") final String quarkusApplicationName) {
        this.debeziumConfiguration = Objects.requireNonNull(debeziumConfiguration);
        this.bootstrapServers = Objects.requireNonNull(bootstrapServers);
        this.quarkusApplicationName = Objects.requireNonNull(quarkusApplicationName);
    }

    public void onStart(@Observes @Priority(50) final StartupEvent ev) throws InvalidTopicsException {
        if (debeziumConfiguration.enabled()) {
            try (final AdminClient adminClient = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
                final List<String> topicsToCheck = List.of(
                        "pulse.%s.t_event".formatted(quarkusApplicationName.toLowerCase()),
                        "pulse.%s.t_aggregate_root".formatted(quarkusApplicationName.toLowerCase()));
                final List<String> topicsPresents = adminClient.listTopics().listings().get()
                        .stream().map(TopicListing::name)
                        .filter(topicsToCheck::contains)
                        .toList();
                final Integer expectedTopicPartitions = debeziumConfiguration.topicCreation().defaultPartitions();
                final DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(topicsPresents);
                final Set<InvalidTopic> invalidTopics = new HashSet<>();
                for (final Map.Entry<String, KafkaFuture<TopicDescription>> entry : describeTopicsResult.topicNameValues().entrySet()) {
                    final String topic = entry.getKey();
                    final KafkaFuture<TopicDescription> topicDescriptionKafkaFuture = entry.getValue();
                    final TopicDescription topicDescription = topicDescriptionKafkaFuture.get();
                    final Integer nbOfPartitions = topicDescription.partitions().size();
                    if (!expectedTopicPartitions.equals(nbOfPartitions)) {
                        invalidTopics.add(new InvalidTopic(topic, nbOfPartitions));
                    }
                }
                if (!invalidTopics.isEmpty()) {
                    throw new InvalidTopicsException(invalidTopics);
                }
            } catch (final ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
