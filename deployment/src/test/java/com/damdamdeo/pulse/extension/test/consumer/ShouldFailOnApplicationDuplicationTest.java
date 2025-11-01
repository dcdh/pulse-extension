package com.damdamdeo.pulse.extension.test.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.*;
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

class ShouldFailOnApplicationDuplicationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-messaging-kafka", Version.getVersion())))
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("functionalDomain 'TodoTaking' componentName 'Todo' declared more than once '2' in target 'statistics'")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    @ApplicationScoped
    @EventChannel(target = "statistics",
            sources = {
                    @EventChannel.Source(functionalDomain = "TodoTaking", componentName = "Todo"),
                    @EventChannel.Source(functionalDomain = "TodoTaking", componentName = "Todo")
            })
    static final class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

        @Override
        public void handleMessage(final Target target,
                                  final AggregateRootType aggregateRootType,
                                  final AggregateId aggregateId,
                                  final CurrentVersionInConsumption currentVersionInConsumption,
                                  final Instant creationDate,
                                  final EventType eventType,
                                  final EncryptedPayload encryptedPayload,
                                  final OwnedBy ownedBy,
                                  final DecryptablePayload<JsonNode> decryptableEventPayload,
                                  final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
        }
    }
}
