package com.damdamdeo.pulse.extension.core.executedby;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class ExecutedByTest {

    @Test
    void encodeEndUser() {
        // Given
        final ExecutedBy executedBy = new ExecutedBy.EndUser("alice");

        // When
        final String encoded = executedBy.encode();

        // Then
        assertThat(encoded).isEqualTo("EU:alice");
    }

    @Test
    void encodeServiceAccount() {
        // Given
        final ExecutedBy executedBy = new ExecutedBy.ServiceAccount("cron-job");

        // When
        final String encoded = executedBy.encode();

        // Then
        assertThat(encoded).isEqualTo("SA:cron-job");
    }

    @Test
    void encodeAnonymous() {
        // Given
        final ExecutedBy executedBy = ExecutedBy.Anonymous.INSTANCE;

        // When
        final String encoded = executedBy.encode();

        // Then
        assertThat(encoded).isEqualTo("A");
    }

    @Test
    void encodeNotAvailable() {
        // Given
        final ExecutedBy executedBy = ExecutedBy.NotAvailable.INSTANCE;

        // When
        final String encoded = executedBy.encode();

        // Then
        assertThat(encoded).isEqualTo("NA");
    }

    @Test
    void decodeEndUser() {
        // Given
        final String value = "EU:alice";

        // When
        final ExecutedBy executedBy = ExecutedBy.decode(value);

        // Then
        assertAll(
                () -> assertThat(executedBy).isInstanceOf(ExecutedBy.EndUser.class),
                () -> assertThat(((ExecutedBy.EndUser) executedBy).by()).isEqualTo("alice"));
    }

    @Test
    void decodeServiceAccount() {
        // Given
        final String value = "SA:cron-job";

        // When
        final ExecutedBy executedBy = ExecutedBy.decode(value);

        // Then
        assertAll(
                () -> assertThat(executedBy).isInstanceOf(ExecutedBy.ServiceAccount.class),
                () -> assertThat(((ExecutedBy.ServiceAccount) executedBy).by()).isEqualTo("cron-job"));
    }

    @Test
    void decodeAnonymous() {
        // Given
        final String value = "A";

        // When
        final ExecutedBy executedBy = ExecutedBy.decode(value);

        // Then
        assertThat(executedBy).isSameAs(ExecutedBy.Anonymous.INSTANCE);
    }

    @Test
    void decodeNotAvailable() {
        // Given
        final String value = "NA";

        // When
        final ExecutedBy executedBy = ExecutedBy.decode(value);

        // Then
        assertThat(executedBy).isSameAs(ExecutedBy.NotAvailable.INSTANCE);
    }

    @Test
    void decodeNullReturnsUnknown() {
        // Given
        final String value = null;

        // When
        final ExecutedBy executedBy = ExecutedBy.decode(value);

        // Then
        assertThat(executedBy).isSameAs(ExecutedBy.NotAvailable.INSTANCE);
    }

    @Test
    void decodeInvalidThrowsException() {
        // Given
        final String value = "INVALID";

        // When / Then
        assertThatThrownBy(() -> ExecutedBy.decode(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ExecutedBy value");
    }

    @Test
    void endUserCannotBeBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new ExecutedBy.EndUser(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("by must not be blank");
    }

    @Test
    void serviceAccountCannotBeBlank() {
        // Given / When / Then
        assertThatThrownBy(() -> new ExecutedBy.ServiceAccount("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("by must not be blank");
    }
}