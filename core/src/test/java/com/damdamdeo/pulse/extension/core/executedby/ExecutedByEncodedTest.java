package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
public class ExecutedByEncodedTest {

    @Mock
    UsernameDecoder usernameDecoder;

    @Test
    void decodeEndUser() throws UnableToDecodeException {
        // Given
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("EU:encodedalice");
        Mockito.doReturn(new Username("alice@mail.com")).when(usernameDecoder).decode(new UsernameEncoded("encodedalice"), Todo.OWNED_BY_USER_1);

        // When
        final ExecutedBy executedBy = executedByEncoded.to(usernameDecoder, Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isInstanceOf(ExecutedBy.EndUser.class),
                () -> assertThat(executedBy.username()).isEqualTo(new Username("alice@mail.com")),
                () -> assertThat(executedBy.value()).isEqualTo("EU:alice@mail.com"));
    }

    @Test
    void shouldEndUserNotBeDecodedWhenPassphraseDoesNotExistAnymore() throws UnableToDecodeException {
        // Given
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("EU:encodedalice");
        Mockito.doThrow(new UnableToDecodeException(new RuntimeException())).when(usernameDecoder)
                .decode(new UsernameEncoded("encodedalice"), Todo.OWNED_BY_USER_1);

        // When / Then
        assertThatThrownBy(() -> executedByEncoded.to(usernameDecoder, Todo.OWNED_BY_USER_1))
                .isExactlyInstanceOf(UnableToDecodeException.class)
                .cause()
                .isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    void decodeServiceAccount() throws UnableToDecodeException {
        // Given
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("SA:cron-job");

        // When
        final ExecutedBy executedBy = executedByEncoded.to(usernameDecoder, Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isInstanceOf(ExecutedBy.ServiceAccount.class),
                () -> assertThat(((ExecutedBy.ServiceAccount) executedBy).by()).isEqualTo("cron-job"),
                () -> assertThat(executedBy.value()).isEqualTo("SA:cron-job"));
    }

    @Test
    void decodeAnonymous() throws UnableToDecodeException {
        // Given
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("A");

        // When
        final ExecutedBy executedBy = executedByEncoded.to(usernameDecoder, Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isSameAs(ExecutedBy.Anonymous.INSTANCE),
                () -> assertThat(executedBy.value()).isEqualTo("A"));
    }

    @Test
    void decodeNotAvailable() throws UnableToDecodeException {
        // Given
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("NA");

        // When
        final ExecutedBy executedBy = executedByEncoded.to(usernameDecoder, Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(executedBy).isSameAs(ExecutedBy.NotAvailable.INSTANCE),
                () -> assertThat(executedBy.value()).isEqualTo("NA"));
    }

}
