package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenAggregateRootIsNotUniqueTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessage("AggregateRoot 'Todo' declared more than once '3'")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    public static class PackageOne {

        public static final class Todo extends AggregateRoot<TodoId> {

            public Todo(final TodoId id) {
                super(id);
            }

            @Override
            public BelongsTo belongsTo() {
                throw new IllegalStateException("Should not be called !");
            }

            @Override
            public OwnedBy ownedBy() {
                throw new IllegalStateException("Should not be called !");
            }
        }
    }

    public static class PackageTwo {

        public static final class Todo extends AggregateRoot<TodoId> {

            Todo(final TodoId id) {
                super(id);
            }

            @Override
            public BelongsTo belongsTo() {
                throw new IllegalStateException("Should not be called !");
            }

            @Override
            public OwnedBy ownedBy() {
                throw new IllegalStateException("Should not be called !");
            }
        }
    }
}
