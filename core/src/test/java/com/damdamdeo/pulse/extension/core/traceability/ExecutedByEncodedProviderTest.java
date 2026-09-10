package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutedByEncodedProviderTest {

    private static final AggregateId AGGREGATE_ID = mock(AggregateId.class);
    private static final OwnedBy OWNED_BY = new OwnedBy("owner");

    @Mock
    private ExecutedByEncodedRepository executedByEncodedRepository;

    @Mock
    private OwnedByProvider ownedByProvider;

    @Mock
    private com.damdamdeo.pulse.extension.core.executedby.UsernameEncoder usernameEncoder;

    @Mock
    private com.damdamdeo.pulse.extension.core.executedby.UsernameHasher usernameHasher;

    @Mock
    private ExecutedBy executedBy;

    @Mock
    private ExecutedByHashed executedByHashed;

    @Mock
    private ExecutedByEncoded executedByEncoded;

    private ExecutedByEncodedProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ExecutedByEncodedProvider(executedByEncodedRepository, ownedByProvider, usernameEncoder,
                usernameHasher);
    }

    @Test
    void shouldReturnExistingEncodedValue() throws Exception {
        // Given
        when(executedBy.hash(usernameHasher)).thenReturn(executedByHashed);
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(executedByEncoded);

        // When
        final ExecutedByEncoded result = provider.provide(AGGREGATE_ID, executedBy);

        // Then
        assertAll(
                () -> assertThat(result).isSameAs(executedByEncoded),
                () -> verify(executedBy).hash(usernameHasher),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(executedBy, never()).encode(any(), any()),
                () -> verify(ownedByProvider, never()).provide(any()),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldEncodeAndStoreWhenEncodedValueDoesNotExist() throws Exception {
        // Given
        when(executedBy.hash(usernameHasher)).thenReturn(executedByHashed);
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        when(ownedByProvider.provide(AGGREGATE_ID)).thenReturn(OWNED_BY);
        when(executedBy.encode(usernameEncoder, OWNED_BY)).thenReturn(executedByEncoded);

        // When
        final ExecutedByEncoded result = provider.provide(AGGREGATE_ID, executedBy);

        // Then
        assertAll(
                () -> assertThat(result).isSameAs(executedByEncoded),
                () -> verify(executedBy).hash(usernameHasher),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(executedBy).encode(usernameEncoder, OWNED_BY),
                () -> verify(executedByEncodedRepository).store(executedByHashed, executedByEncoded)
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenEncodingFails() throws Exception {
        // Given
        when(executedBy.hash(usernameHasher)).thenReturn(executedByHashed);
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        when(ownedByProvider.provide(AGGREGATE_ID)).thenReturn(OWNED_BY);
        final UnableToEncodeException cause = new UnableToEncodeException(new IllegalStateException("Unable to encode"));
        when(executedBy.encode(usernameEncoder, OWNED_BY)).thenThrow(cause);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, executedBy))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(executedBy).hash(usernameHasher),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(executedBy).encode(usernameEncoder, OWNED_BY),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenRepositoryFindFails() throws Exception {
        // Given
        when(executedBy.hash(usernameHasher)).thenReturn(executedByHashed);
        final ExecutedByEncodedRepositoryException cause = new ExecutedByEncodedRepositoryException(
                new IllegalStateException("Repository failure"));
        when(executedByEncodedRepository.findBy(executedByHashed)).thenThrow(cause);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, executedBy))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(executedBy).hash(usernameHasher),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider, never()).provide(any()),
                () -> verify(executedBy, never()).encode(any(), any()),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenRepositoryStoreFails() throws Exception {
        // Given
        when(executedBy.hash(usernameHasher)).thenReturn(executedByHashed);
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        when(ownedByProvider.provide(AGGREGATE_ID)).thenReturn(OWNED_BY);
        when(executedBy.encode(usernameEncoder, OWNED_BY)).thenReturn(executedByEncoded);
        final ExecutedByEncodedRepositoryException cause = new ExecutedByEncodedRepositoryException(
                new IllegalStateException("Repository failure"));
        doThrow(cause).when(executedByEncodedRepository).store(executedByHashed, executedByEncoded);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, executedBy))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(executedBy).hash(usernameHasher),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(executedBy).encode(usernameEncoder, OWNED_BY),
                () -> verify(executedByEncodedRepository).store(executedByHashed, executedByEncoded)
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenOwnedByCannotBeProvided() throws Exception {
        // Given
        when(executedBy.hash(usernameHasher)).thenReturn(executedByHashed);
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        final OwnedByProviderException cause = new OwnedByProviderException(
                new IllegalStateException("Unable to provide owner"));
        when(ownedByProvider.provide(AGGREGATE_ID)).thenThrow(cause);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, executedBy))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(executedBy).hash(usernameHasher),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(executedBy, never()).encode(any(), any()),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }
}
