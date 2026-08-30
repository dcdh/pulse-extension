package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutedByTest {

    UsernameEncoder usernameEncoder = new TestUsernameEncoder();

    @Nested
    class Encode {

        @Test
        void encodeEndUser() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = new ExecutedBy.EndUser(new Username("alice@mail.com"));

            // When
            final ExecutedByEncoded encoded = executedBy.encode(usernameEncoder, Todo.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo(new ExecutedByEncoded("EU:encoded"));
        }

        @Test
        void encodeServiceAccount() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = new ExecutedBy.ServiceAccount("cron-job");

            // When
            final ExecutedByEncoded encoded = executedBy.encode(usernameEncoder, Todo.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo(new ExecutedByEncoded("SA:cron-job"));
        }

        @Test
        void encodeAnonymous() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;

            // When
            final ExecutedByEncoded encoded = executedBy.encode(usernameEncoder, Todo.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo(new ExecutedByEncoded("A"));
        }

        @Test
        void encodeNotAvailable() throws UnableToEncodeException {
            // Given
            final ExecutedBy executedBy = ExecutedBy.NotAvailable.INSTANCE;

            // When
            final ExecutedByEncoded encoded = executedBy.encode(usernameEncoder, Todo.OWNED_BY_USER_1);

            // Then
            assertThat(encoded).isEqualTo(new ExecutedByEncoded("NA"));
        }
    }

    @Nested
    class EndUser {

        @Test
        void shouldReturnUsername() {
            // Given / When / Then
            assertThat(new ExecutedBy.EndUser(new Username("bob@mail.com")).username()).isEqualTo(new Username("bob@mail.com"));
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

        @Test
        void shouldThrowExceptionOnUsername() {
            // Given / When / Then
            assertThatThrownBy(() -> new ExecutedBy.ServiceAccount("cron-job").username())
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Service account does not have a username");
        }
    }

    @Nested
    class Anonymous {

        @Test
        void shouldThrowExceptionOnUsername() {
            // Given / When / Then
            assertThatThrownBy(ExecutedBy.Anonymous.INSTANCE::username)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Anonymous does not have a username");
        }
    }

    @Nested
    class NotAvailable {

        @Test
        void shouldThrowExceptionOnUsername() {
            // Given / When / Then
            assertThatThrownBy(ExecutedBy.NotAvailable.INSTANCE::username)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Not available does not have a username");
        }
    }
}
