package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.User;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutedByTest {

    ExecutedByEncoder executedByEncoder = new TestExecutedByEncoder();

    @Nested
    class Encode {

        @Test
        void encodeEndUser() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = new ExecutedBy.EndUser("alice", true);

            // When
            final String encoded = executedBy.encode(executedByEncoder, User.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo("EU:encodedalice");
        }

        @Test
        void encodeServiceAccount() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = new ExecutedBy.ServiceAccount("cron-job");

            // When
            final String encoded = executedBy.encode(executedByEncoder, User.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo("SA:cron-job");
        }

        @Test
        void encodeAnonymous() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;

            // When
            final String encoded = executedBy.encode(executedByEncoder, User.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo("A");
        }

        @Test
        void encodeNotAvailable() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = ExecutedBy.NotAvailable.INSTANCE;

            // When
            final String encoded = executedBy.encode(executedByEncoder, User.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo("NA");
        }
    }

    @Nested
    class EndUser {

        @Test
        void endUserCannotBeBlank() {
            // Given / When / Then
            assertThatThrownBy(() -> new ExecutedBy.EndUser("", true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("by must not be blank");
        }
    }

    @Nested
    class ServiceAccount {

        @Test
        void serviceAccountCannotBeBlank() {
            // Given / When / Then
            assertThatThrownBy(() -> new ExecutedBy.ServiceAccount("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("by must not be blank");
        }
    }
}
