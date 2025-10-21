package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.event.Event;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenEventNotARecordTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessage("Event 'com.damdamdeo.pulse.extension.test.ShouldFailWhenEventNotARecordTest$InvalidEvent' must be a record")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    // NOT needed to use addClass
    // Will be registered automatically
    private static final class InvalidEvent implements Event<TodoId> {

        @Override
        public TodoId id() {
            throw new RuntimeException("Should not have been called");
        }

        @Override
        public OwnedBy ownedBy() {
            throw new RuntimeException("Should not have been called");
        }
    }

}
