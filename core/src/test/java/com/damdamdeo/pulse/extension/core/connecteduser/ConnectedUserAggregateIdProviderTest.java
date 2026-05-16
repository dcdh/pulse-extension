package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AlreadyAssociatedException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import com.damdamdeo.pulse.extension.core.connectionidentifier.UnableToFindByHashException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Function;

import static com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociationTest.GIVEN_HASH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectedUserAggregateIdProviderTest {

    public static final ConnectedUser CONNECTED_USER = new ConnectedUser(new Username("damien.clementdhuart@gmail.com"));

    @Mock
    ConnectedUserProvider connectedUserProvider;

    @Mock
    AggregateIdGenerator aggregateIdGenerator;

    @Mock
    ConnectionAssociationFinder connectionAssociationFinder;

    @Mock
    ConnectionIdentifierAssociation connectionIdentifierAssociation;

    @Mock
    Hasher hasher;

    @InjectMocks
    ConnectedUserAggregateIdProvider connectedUserAggregateIdProvider;

    private Function<Identifiable, TodoId> creationalFromIdentifiable = TodoId::from;
    private Function<SequenceNumber, TodoId> creationalFromSequenceNumber = sequenceNumber -> new TodoId("Damien", sequenceNumber);

    @Test
    void shouldReturnExistingAggregateIdWhenAssociationExists() throws Exception {
        // Given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);

        doReturn(Optional.of(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1))).when(connectionAssociationFinder).findByHash(GIVEN_HASH, creationalFromIdentifiable);

        // When
        final TodoId result = connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable,
                creationalFromSequenceNumber);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1)),
                () -> verify(connectionIdentifierAssociation, never()).associate(any(), any()),
                () -> verify(aggregateIdGenerator, never()).generate(eq(TodoId.class), any()),
                () -> verify(aggregateIdGenerator, never()).generate(any(For.class), any()));
    }

    @Test
    void shouldGenerateAndAssociateAggregateIdWhenAssociationDoesNotExist() throws Exception {
        // Given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);
        doReturn(Optional.empty()).when(connectionAssociationFinder).findByHash(any(), any());

        doReturn(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_2)).when(aggregateIdGenerator).generate(TodoId.class, creationalFromSequenceNumber);

        // When
        final TodoId result = connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable,
                creationalFromSequenceNumber);

        // Then
        assertThat(result).isEqualTo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_2));
    }

    @Test
    void shouldThrowExceptionWhenUnableToFindByHash() throws Exception {
        // Given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);

        doThrow(new UnableToFindByHashException(new RuntimeException(), GIVEN_HASH)).when(connectionAssociationFinder).findByHash(any(), any());

        // When && Then
        assertThatThrownBy(() -> connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable, creationalFromSequenceNumber))
                .isInstanceOf(ConnectedUserAggregateIdProviderException.class)
                .cause()
                .isInstanceOf(UnableToFindByHashException.class)
                .hasFieldOrPropertyWithValue("connectionIdentifierHash", GIVEN_HASH);
    }

    @Test
    void shouldThrowExceptionWhenSequenceGenerationFails() throws Exception {
        // given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);
        doReturn(Optional.empty()).when(connectionAssociationFinder).findByHash(any(), any());
        doThrow(new SequenceGenerationException("fail", "mySequence")).when(aggregateIdGenerator).generate(eq(TodoId.class), any());

        // When && Then
        assertThatThrownBy(() -> connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable, creationalFromSequenceNumber))
                .isInstanceOf(ConnectedUserAggregateIdProviderException.class)
                .cause()
                .isInstanceOf(SequenceGenerationException.class)
                .hasFieldOrPropertyWithValue("sequenceName", "mySequence");
    }

    @Test
    void should_throw_exception_when_association_fails() throws Exception {
        // Given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);
        doReturn(Optional.empty()).when(connectionAssociationFinder).findByHash(any(), any());
        doReturn(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1)).when(aggregateIdGenerator).generate(eq(TodoId.class), any());

        doThrow(new AlreadyAssociatedException(CONNECTED_USER)).when(connectionIdentifierAssociation)
                .associate(CONNECTED_USER, new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1));

        // When && Then
        assertThatThrownBy(() -> connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable, creationalFromSequenceNumber))
                .isInstanceOf(ConnectedUserAggregateIdProviderException.class)
                .cause()
                .isInstanceOf(AlreadyAssociatedException.class)
                .hasFieldOrPropertyWithValue("connectionIdentifier", CONNECTED_USER);
    }

}
