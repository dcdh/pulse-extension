package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.core.traceability.CorrelationId;
import com.damdamdeo.pulse.extension.traceability.runtime.ContextLocalsCorrelationIdProviderContextualStorage;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContextLocalsCorrelationIdProviderContextualStorageTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("pulse.traceability.tracing-mode", "INVOLVED_WITH_FULL_DETAILS")
            .withConfigurationResource("application.properties");

    @Inject
    ContextLocalsCorrelationIdProviderContextualStorage contextLocalsCorrelationIdProviderContextualStorage;

    @Test
    @Order(1)
    @RunOnVertxContext
    void shouldReturnEmptyWhenNoCorrelationIdPresent() {
        // Given

        // When
        final Optional<CorrelationId> retrieve = contextLocalsCorrelationIdProviderContextualStorage.retrieve();

        // Then
        assertThat(retrieve).isEqualTo(Optional.empty());
    }

    @Test
    @Order(2)
    @RunOnVertxContext
    void shouldStoreCorrelationIdAndReturnIt() {
        // Given

        // When
        final CorrelationId stored = contextLocalsCorrelationIdProviderContextualStorage.store(new CorrelationId(123L));

        // Then
        assertAll(
                () -> assertThat(stored).isEqualTo(new CorrelationId(123L)),
                () -> assertThat(contextLocalsCorrelationIdProviderContextualStorage.retrieve()).isEqualTo(
                        Optional.of(new CorrelationId(123L)))
        );
    }
}
