package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.AggregateId;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenAggregateIdNotARecordTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessage("AggregateId 'com.damdamdeo.pulse.extension.test.ShouldFailWhenAggregateIdNotARecordTest$InvalidAggregateId' must be a record")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    // NOT needed to use addClass
    // Will be registered automatically
    private static final class InvalidAggregateId implements AggregateId {

        @Override
        public String id() {
            throw new RuntimeException("Should not have been called");
        }
    }
}
