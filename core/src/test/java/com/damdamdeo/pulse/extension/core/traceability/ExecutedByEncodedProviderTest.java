package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.*;
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
    private static final Username USERNAME = new Username("bob@mail.com");

    private static final ExecutedBy EXECUTED_BY = new ExecutedBy.EndUser(USERNAME);

    @Mock
    private ExecutedByEncodedRepository executedByEncodedRepository;

    @Mock
    private OwnedByProvider ownedByProvider;

    @Mock
    private UsernameEncoder usernameEncoder;

    @Mock
    private UsernameHasher usernameHasher;

    @Mock
    private ExecutedByHashed executedByHashed;

    @Mock
    private UsernameHashed usernameHashed;

    @Mock
    private ExecutedByEncoded executedByEncoded;

    @Mock
    private UsernameEncoded usernameEncoded;

    private ExecutedByEncodedProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ExecutedByEncodedProvider(executedByEncodedRepository, ownedByProvider, usernameEncoder,
                usernameHasher);
    }

    @Test
    void shouldReturnExistingEncodedValue() throws Exception {
        // Given
        when(usernameHashed.hashed()).thenReturn("hashed-bob");
        when(usernameHasher.hash(USERNAME)).thenReturn(usernameHashed);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed-bob");
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(executedByEncoded);

        // When
        final ExecutedByEncoded result = provider.provide(AGGREGATE_ID, EXECUTED_BY);

        // Then
        assertAll(
                () -> assertThat(result).isSameAs(executedByEncoded),
                () -> verify(usernameHasher).hash(USERNAME),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(usernameEncoder, never()).encode(any(), any()),
                () -> verify(ownedByProvider, never()).provide(any()),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldEncodeAndStoreWhenEncodedValueDoesNotExist() throws Exception {
        // Given
        when(usernameHashed.hashed()).thenReturn("hashed-bob");
        when(usernameHasher.hash(USERNAME)).thenReturn(usernameHashed);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed-bob");
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        when(ownedByProvider.provide(AGGREGATE_ID)).thenReturn(OWNED_BY);
        when(usernameEncoder.encode(USERNAME, OWNED_BY)).thenReturn(usernameEncoded);
        when(usernameEncoded.encoded()).thenReturn("encoded-bob");

        // When
        final ExecutedByEncoded result = provider.provide(AGGREGATE_ID, EXECUTED_BY);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(new ExecutedByEncoded("EU:encoded-bob")),
                () -> verify(usernameHasher).hash(USERNAME),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(usernameEncoder).encode(USERNAME, OWNED_BY),
                () -> verify(usernameEncoded).encoded(),
                () -> verify(executedByEncodedRepository)
                        .store(executedByHashed, new ExecutedByEncoded("EU:encoded-bob"))
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenEncodingFails() throws Exception {
        // Given
        when(usernameHashed.hashed()).thenReturn("hashed-bob");
        when(usernameHasher.hash(USERNAME)).thenReturn(usernameHashed);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed-bob");
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        when(ownedByProvider.provide(AGGREGATE_ID)).thenReturn(OWNED_BY);
        final UnableToEncodeException cause = new UnableToEncodeException(
                new IllegalStateException("Unable to encode"));
        when(usernameEncoder.encode(USERNAME, OWNED_BY)).thenThrow(cause);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, EXECUTED_BY))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(usernameHasher).hash(USERNAME),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(usernameEncoder).encode(USERNAME, OWNED_BY),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenRepositoryFindFails() throws Exception {
        // Given
        when(usernameHashed.hashed()).thenReturn("hashed-bob");
        when(usernameHasher.hash(USERNAME)).thenReturn(usernameHashed);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed-bob");
        final ExecutedByEncodedRepositoryException cause = new ExecutedByEncodedRepositoryException(
                new IllegalStateException("Repository failure"));
        when(executedByEncodedRepository.findBy(executedByHashed)).thenThrow(cause);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, EXECUTED_BY))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(usernameHasher).hash(USERNAME),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider, never()).provide(any()),
                () -> verify(usernameEncoder, never()).encode(any(), any()),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenRepositoryStoreFails() throws Exception {
        // Given
        when(usernameHashed.hashed()).thenReturn("hashed-bob");
        when(usernameHasher.hash(USERNAME)).thenReturn(usernameHashed);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed-bob");
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        when(ownedByProvider.provide(AGGREGATE_ID)).thenReturn(OWNED_BY);
        when(usernameEncoder.encode(USERNAME, OWNED_BY)).thenReturn(usernameEncoded);
        when(usernameEncoded.encoded()).thenReturn("encoded-bob");
        final ExecutedByEncoded encoded = new ExecutedByEncoded("EU:encoded-bob");
        final ExecutedByEncodedRepositoryException cause = new ExecutedByEncodedRepositoryException(
                new IllegalStateException("Repository failure"));

        doThrow(cause)
                .when(executedByEncodedRepository)
                .store(executedByHashed, encoded);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, EXECUTED_BY))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(usernameHasher).hash(USERNAME),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(usernameEncoder).encode(USERNAME, OWNED_BY),
                () -> verify(usernameEncoded).encoded(),
                () -> verify(executedByEncodedRepository).store(executedByHashed, encoded)
        );
    }

    @Test
    void shouldThrowExecutedByEncoderExceptionWhenOwnedByCannotBeProvided() throws Exception {
        // Given
        when(usernameHashed.hashed()).thenReturn("hashed-bob");
        when(usernameHasher.hash(USERNAME)).thenReturn(usernameHashed);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed-bob");
        when(executedByEncodedRepository.findBy(executedByHashed)).thenReturn(null);
        final OwnedByProviderException cause = new OwnedByProviderException(
                new IllegalStateException("Unable to provide owner"));
        when(ownedByProvider.provide(AGGREGATE_ID)).thenThrow(cause);

        // When / Then
        assertAll(
                () -> assertThatThrownBy(() -> provider.provide(AGGREGATE_ID, EXECUTED_BY))
                        .isInstanceOf(ExecutedByEncoderException.class)
                        .hasCause(cause),
                () -> verify(usernameHasher).hash(USERNAME),
                () -> verify(executedByEncodedRepository).findBy(executedByHashed),
                () -> verify(ownedByProvider).provide(AGGREGATE_ID),
                () -> verify(usernameEncoder, never()).encode(any(), any()),
                () -> verify(executedByEncodedRepository, never()).store(any(), any())
        );
    }
}
