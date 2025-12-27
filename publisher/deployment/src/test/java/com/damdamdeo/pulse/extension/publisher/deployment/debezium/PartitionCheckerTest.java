package com.damdamdeo.pulse.extension.publisher.deployment.debezium;

import com.damdamdeo.pulse.extension.publisher.runtime.debezium.DebeziumConfigurator;
import com.damdamdeo.pulse.extension.publisher.runtime.debezium.InvalidTopic;
import com.damdamdeo.pulse.extension.publisher.runtime.debezium.InvalidTopicsException;
import com.damdamdeo.pulse.extension.publisher.runtime.debezium.PartitionChecker;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PartitionCheckerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.arc.exclude-types", DebeziumConfigurator.class.getName())
            .overrideConfigKey("pulse.debezium.topic-creation.default-partitions", "2");

    @Inject
    PartitionChecker partitionChecker;

    @Test
    void shouldFailWhenPartitionsMismatched() {
        // Given
        try (final AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class)))) {
            final CreateTopicsResult topics = adminClient.createTopics(List.of(
                    new NewTopic("pulse.todotaking_todo.t_event", 3, (short) 1),
                    new NewTopic("pulse.todotaking_todo.t_aggregate_root", 4, (short) 1)));
            topics.all().get();
        } catch (final ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // When && Then
        assertThatThrownBy(() -> partitionChecker.onStart(new StartupEvent()))
                .isExactlyInstanceOf(InvalidTopicsException.class)
                .hasFieldOrPropertyWithValue("invalidTopics", Set.of(
                        new InvalidTopic("pulse.todotaking_todo.t_aggregate_root", 4),
                        new InvalidTopic("pulse.todotaking_todo.t_event", 3)));
    }
}
