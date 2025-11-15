package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.writer.runtime.PostgresqlEventStoreInitializer;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class ShouldFailWhenEventIsNotUniqueTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.arc.exclude-types",
                    "%s,%s".formatted(PostgresqlSchemaInitializer.class.getName(), PostgresqlEventStoreInitializer.class.getName()))
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessage("Event 'NewTodoCreated' declared more than once '2'")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    public record NewTodoCreated(String description) implements Event {

        public NewTodoCreated {
            Objects.requireNonNull(description);
        }
    }
}

