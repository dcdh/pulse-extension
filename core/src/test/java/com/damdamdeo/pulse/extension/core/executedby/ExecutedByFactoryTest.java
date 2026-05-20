package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
public class ExecutedByFactoryTest {

    @Mock
    ExecutedByDecoder executedByDecoder;

    @InjectMocks
    ExecutedByFactory executedByFactory;

    @Test
    void decodeEndUser() throws UnableToDecodeException {
        // Given
        final String value = "EU:encodedalice";
        Mockito.doReturn(Optional.of("alice")).when(executedByDecoder).decode("encodedalice", User.OWNED_BY_USER_1);

        // When
        final ExecutedBy executedBy = executedByFactory.from(value, User.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isInstanceOf(ExecutedBy.EndUser.class),
                () -> assertThat(((ExecutedBy.EndUser) executedBy).by()).isEqualTo("alice"),
                () -> assertThat(executedBy.value()).isEqualTo("EU:alice"),
                () -> assertThat(executedBy.decoded()).isTrue());
    }

    @Test
    void shouldEndUserNotBeDecodedWhenPassphraseDoesNotExistAnymore() throws UnableToDecodeException {
        // Given
        final String value = "EU:encodedalice";
        Mockito.doReturn(Optional.empty()).when(executedByDecoder).decode("encodedalice", User.OWNED_BY_USER_1);

        // When
        final ExecutedBy executedBy = executedByFactory.from(value, User.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isInstanceOf(ExecutedBy.EndUser.class),
                () -> assertThat(((ExecutedBy.EndUser) executedBy).by()).isEqualTo("encodedalice"),
                () -> assertThat(executedBy.value()).isEqualTo("EU:encodedalice"),
                () -> assertThat(executedBy.decoded()).isFalse());
    }

    @Test
    void decodeServiceAccount() throws UnableToDecodeException {
        // Given
        final String value = "SA:cron-job";

        // When
        final ExecutedBy executedBy = executedByFactory.from(value, User.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isInstanceOf(ExecutedBy.ServiceAccount.class),
                () -> assertThat(((ExecutedBy.ServiceAccount) executedBy).by()).isEqualTo("cron-job"),
                () -> assertThat(executedBy.value()).isEqualTo("SA:cron-job"),
                () -> assertThat(executedBy.decoded()).isTrue());
    }

    @Test
    void decodeAnonymous() throws UnableToDecodeException {
        // Given
        final String value = "A";

        // When
        final ExecutedBy executedBy = executedByFactory.from(value, User.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isSameAs(ExecutedBy.Anonymous.INSTANCE),
                () -> assertThat(executedBy.value()).isEqualTo("A"),
                () -> assertThat(executedBy.decoded()).isTrue());
    }

    @Test
    void decodeNotAvailable() throws UnableToDecodeException {
        // Given
        final String value = "NA";

        // When
        final ExecutedBy executedBy = executedByFactory.from(value, User.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isSameAs(ExecutedBy.NotAvailable.INSTANCE),
                () -> assertThat(executedBy.value()).isEqualTo("NA"),
                () -> assertThat(executedBy.decoded()).isTrue());
    }

    @Test
    void decodeNullReturnsNotAvailable() throws UnableToDecodeException {
        // Given
        final String value = null;

        // When
        final ExecutedBy executedBy = executedByFactory.from(value, User.OWNED_BY_USER_1);

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
        assertThatThrownBy(() -> executedByFactory.from(value, User.OWNED_BY_USER_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ExecutedBy value");
    }

}
