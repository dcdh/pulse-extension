package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubBackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubExecutedByResolver;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class ShouldFailWhenEventIsNotUniqueTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            // classes.add(GuardDomainUseCase.class);
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(
                    StubBackendUserVisibilityRolesProvider.class, StubExecutedByResolver.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
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

    public record NewTodoCreated(String description) implements Event<TodoId> {

        public NewTodoCreated {
            Objects.requireNonNull(description);
        }
    }
}

