package com.damdamdeo.pulse.extension.livenotifier;

import io.smallrye.reactive.messaging.kafka.companion.TopicsCompanion;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

public class TopicManager {

    public static void resetTopics() {
        final String bootstrapServers = ConfigProvider.getConfig()
                .getValue("kafka.bootstrap.servers", String.class);

        try (final AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            final TopicsCompanion topicsCompanion = new TopicsCompanion(adminClient, Duration.ofSeconds(10));
            final Set<String> topics = topicsCompanion.list();
            topicsCompanion.delete(topics);
            topics.forEach(topic -> topicsCompanion.createAndWait(topic, 1));
        }
    }
}
