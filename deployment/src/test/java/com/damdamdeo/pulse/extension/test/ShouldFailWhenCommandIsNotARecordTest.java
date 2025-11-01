package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.TodoId;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenCommandIsNotARecordTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessage("Command 'com.damdamdeo.pulse.extension.test.ShouldFailWhenCommandIsNotARecordTest$InvalidCommand' must be a record")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    // NOT needed to use addClass
    // Will be registered automatically
    private static final class InvalidCommand implements Command<TodoId> {

        @Override
        public TodoId id() {
            throw new RuntimeException("Should not have been called");
        }
    }
}
