package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.writer.runtime.PostgresqlEventStoreInitializer;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenAggregateIdNotARecordTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "false")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .overrideConfigKey("quarkus.arc.exclude-types",
                    "%s,%s".formatted(PostgresqlSchemaInitializer.class.getName(), PostgresqlEventStoreInitializer.class.getName()))
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessage("AggregateId 'com.damdamdeo.pulse.extension.writer.deployment.ShouldFailWhenAggregateIdNotARecordTest$InvalidAggregateId' must be a record")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    @Inject
    CommandHandler<Todo, TodoId> commandHandler;

    // NOT needed to use addClass
    // Will be registered automatically
    private static final class InvalidAggregateId implements AggregateId {

        @Override
        public String id() {
            throw new RuntimeException("Should not have been called");
        }
    }
}
