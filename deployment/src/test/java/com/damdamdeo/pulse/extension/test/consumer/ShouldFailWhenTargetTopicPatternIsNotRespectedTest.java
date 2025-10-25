package com.damdamdeo.pulse.extension.test.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.AsyncEventChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.Target;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.runtime.consumer.EventChannel;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenTargetTopicPatternIsNotRespectedTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("pulse.target-topic-binding.statistics", "statistics0")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Topic naming invalid 'statistics0' - it should match [a-zA-Z]+")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    @ApplicationScoped
    @EventChannel(target = "statistics")
    static final class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

        @Override
        public void handleMessage(final Target target,
                                  final AggregateId aggregateId,
                                  final AggregateRootType aggregateRootType,
                                  final CurrentVersionInConsumption currentVersionInConsumption,
                                  final Instant creationDate,
                                  final EventType eventType,
                                  final EncryptedPayload encryptedPayload,
                                  final OwnedBy ownedBy,
                                  final JsonNode decryptedEventPayload,
                                  final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
        }
    }
}
