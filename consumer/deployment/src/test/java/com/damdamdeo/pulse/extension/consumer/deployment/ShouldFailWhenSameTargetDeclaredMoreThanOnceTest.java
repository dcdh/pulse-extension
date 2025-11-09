package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
import com.damdamdeo.pulse.extension.consumer.runtime.EventChannel;
import com.damdamdeo.pulse.extension.consumer.runtime.JdbcPostgresIdempotencyRepository;
import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
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
            .overrideConfigKey("quarkus.compose.devservices.enabled", "false")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey("quarkus.arc.exclude-types",
                    "%s,%s".formatted(
                            JdbcPostgresIdempotencyRepository.class.getName(),
                            PostgresqlSchemaInitializer.class.getName()))
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
    @EventChannel(target = "statistics",
            sources = {
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

    @ApplicationScoped
    @EventChannel(target = "statistics",
            sources = {
                    @EventChannel.Source(functionalDomain = "TodoTaking", componentName = "Todo")
            })
    static final class StatisticsAgainEventHandler implements AsyncEventChannelMessageHandler<JsonNode> {

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
