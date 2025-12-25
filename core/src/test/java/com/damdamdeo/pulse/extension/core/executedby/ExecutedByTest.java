package com.damdamdeo.pulse.extension.core.executedby;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class ExecutedByTest {

    @Nested
    class Encode {

        @Test
        void encodeEndUser() {
            // Given
            final ExecutedBy executedBy = new ExecutedBy.EndUser("alice", true);

            // When
            final String encoded = executedBy.encode(TestExecutedByEncoder.INSTANCE);

            // Then
            assertThat(encoded).isEqualTo("EU:encodedalice");
        }

        @Test
        void encodeServiceAccount() {
            // Given
            final ExecutedBy executedBy = new ExecutedBy.ServiceAccount("cron-job");

            // When
            final String encoded = executedBy.encode(TestExecutedByEncoder.INSTANCE);

            // Then
            assertThat(encoded).isEqualTo("SA:cron-job");
        }

        @Test
        void encodeAnonymous() {
            // Given
            final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;

            // When
            final String encoded = executedBy.encode(TestExecutedByEncoder.INSTANCE);

            // Then
            assertThat(encoded).isEqualTo("A");
        }

        @Test
        void encodeNotAvailable() {
            // Given
            final ExecutedBy executedBy = ExecutedBy.NotAvailable.INSTANCE;

            // When
            final String encoded = executedBy.encode(TestExecutedByEncoder.INSTANCE);

            // Then
            assertThat(encoded).isEqualTo("NA");
        }
    }

    @Nested
    class Decode {

        @Test
        void decodeEndUser() {
            // Given
            final String value = "EU:encodedalice";

            // When
            final ExecutedBy executedBy = ExecutedBy.decode(value, TestExecutedByDecoder.INSTANCE);

            // Then
            assertAll(
                    () -> assertThat(executedBy).isInstanceOf(ExecutedBy.EndUser.class),
                    () -> assertThat(((ExecutedBy.EndUser) executedBy).by()).isEqualTo("alice"),
                    () -> assertThat(executedBy.value()).isEqualTo("EU:alice"),
                    () -> assertThat(executedBy.decoded()).isTrue());
        }

        @Test
        void shouldEndUserNotBeDecodedWhenPassphraseDoesNotExistAnymore() {
            // Given
            final String value = "EU:encodedalice";

            // When
            final ExecutedBy executedBy = ExecutedBy.decode(value, encoded -> Optional.empty());

            // Then
            assertAll(
                    () -> assertThat(executedBy).isInstanceOf(ExecutedBy.EndUser.class),
                    () -> assertThat(((ExecutedBy.EndUser) executedBy).by()).isEqualTo("encodedalice"),
                    () -> assertThat(executedBy.value()).isEqualTo("EU:encodedalice"),
                    () -> assertThat(executedBy.decoded()).isFalse());
        }

        @Test
        void decodeServiceAccount() {
            // Given
            final String value = "SA:cron-job";

            // When
            final ExecutedBy executedBy = ExecutedBy.decode(value, TestExecutedByDecoder.INSTANCE);

            // Then
            assertAll(
                    () -> assertThat(executedBy).isInstanceOf(ExecutedBy.ServiceAccount.class),
                    () -> assertThat(((ExecutedBy.ServiceAccount) executedBy).by()).isEqualTo("cron-job"),
                    () -> assertThat(executedBy.value()).isEqualTo("SA:cron-job"),
                    () -> assertThat(executedBy.decoded()).isTrue());
        }

        @Test
        void decodeAnonymous() {
            // Given
            final String value = "A";

            // When
            final ExecutedBy executedBy = ExecutedBy.decode(value, TestExecutedByDecoder.INSTANCE);

            // Then
            assertAll(
                    () -> assertThat(executedBy).isSameAs(ExecutedBy.Anonymous.INSTANCE),
                    () -> assertThat(executedBy.value()).isEqualTo("A"),
                    () -> assertThat(executedBy.decoded()).isTrue());
        }

        @Test
        void decodeNotAvailable() {
            // Given
            final String value = "NA";

            // When
            final ExecutedBy executedBy = ExecutedBy.decode(value, TestExecutedByDecoder.INSTANCE);

            // Then
            assertAll(
                    () -> assertThat(executedBy).isSameAs(ExecutedBy.NotAvailable.INSTANCE),
                    () -> assertThat(executedBy.value()).isEqualTo("NA"),
                    () -> assertThat(executedBy.decoded()).isTrue());
        }

        @Test
        void decodeNullReturnsNotAvailable() {
            // Given
            final String value = null;

            // When
            final ExecutedBy executedBy = ExecutedBy.decode(value, TestExecutedByDecoder.INSTANCE);

            // Then
            assertAll(
                    () -> assertThat(executedBy).isSameAs(ExecutedBy.NotAvailable.INSTANCE),
                    () -> assertThat(executedBy.value()).isEqualTo("NA"),
                    () -> assertThat(executedBy.decoded()).isTrue());
        }

        @Test
        void decodeInvalidThrowsException() {
            // Given
            final String value = "INVALID";

            // When / Then
            assertThatThrownBy(() -> ExecutedBy.decode(value, TestExecutedByDecoder.INSTANCE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid ExecutedBy value");
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