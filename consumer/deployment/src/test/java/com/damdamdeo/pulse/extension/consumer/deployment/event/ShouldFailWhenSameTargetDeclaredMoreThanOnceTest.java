package com.damdamdeo.pulse.extension.consumer.deployment.event;

import com.damdamdeo.pulse.extension.consumer.runtime.event.AsyncEventConsumerChannel;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.consumer.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.event.AggregateRootLoaded;
import com.damdamdeo.pulse.extension.core.consumer.event.AsyncEventChannelMessageHandler;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenSameTargetDeclaredMoreThanOnceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Target 'statistics' declared more than once '2'")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    @ApplicationScoped
    @AsyncEventConsumerChannel(target = "statistics",
            sources = {
                    @AsyncEventConsumerChannel.Source(functionalDomain = "TodoClient", componentName = "Registered")
            })
    static final class StatisticsEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

        @Override
        public void handleMessage(final FromApplication fromApplication,
                                  final Target target,
                                  final AggregateRootType aggregateRootType,
                                  final AggregateId aggregateId,
                                  final CurrentVersionInConsumption currentVersionInConsumption,
                                  final Instant creationDate,
                                  final EventType eventType,
                                  final EncryptedPayload encryptedPayload,
                                  final OwnedBy ownedBy,
                                  final ExecutedBy executedBy,
                                  final DecryptablePayload<JsonNode> decryptableEventPayload,
                                  final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
        }
    }

    @ApplicationScoped
    @AsyncEventConsumerChannel(target = "statistics",
            sources = {
                    @AsyncEventConsumerChannel.Source(functionalDomain = "TodoClient", componentName = "Registered")
            })
    static final class StatisticsAgainEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

        @Override
        public void handleMessage(final FromApplication fromApplication,
                                  final Target target,
                                  final AggregateRootType aggregateRootType,
                                  final AggregateId aggregateId,
                                  final CurrentVersionInConsumption currentVersionInConsumption,
                                  final Instant creationDate,
                                  final EventType eventType,
                                  final EncryptedPayload encryptedPayload,
                                  final OwnedBy ownedBy,
                                  final ExecutedBy executedBy,
                                  final DecryptablePayload<JsonNode> decryptableEventPayload,
                                  final Supplier<AggregateRootLoaded<JsonNode>> aggregateRootLoadedSupplier) {
        }
    }
}
