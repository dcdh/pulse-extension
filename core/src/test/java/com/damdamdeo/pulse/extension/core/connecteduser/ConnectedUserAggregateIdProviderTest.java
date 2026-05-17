package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AlreadyAssociatedException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import com.damdamdeo.pulse.extension.core.connectionidentifier.UnableToFindException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectedUserAggregateIdProviderTest {

    public static final ConnectedUser CONNECTED_USER = new ConnectedUser(new Username("damien.clementdhuart@gmail.com"));

    @Mock
    AggregateIdGenerator aggregateIdGenerator;

    @Mock
    ConnectionAssociationFinder connectionAssociationFinder;

    @Mock
    ConnectionIdentifierAssociation connectionIdentifierAssociation;

    @InjectMocks
    ConnectedUserAggregateIdProvider connectedUserAggregateIdProvider;

    private Function<Identifiable, TodoId> creationalFromIdentifiable = TodoId::from;
    private Function<SequenceNumber, TodoId> creationalFromSequenceNumber = sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber);

    @Test
    void shouldReturnExistingAggregateIdWhenAssociationExists() throws Exception {
        // Given
        doReturn(Provided.ofKnown(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), CONNECTED_USER))
                .when(connectionAssociationFinder).findByConnectedUser(creationalFromIdentifiable);

        // When
        final TodoId result = connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable,
                creationalFromSequenceNumber);

        // Then
        assertAll(
                () -> assertThat(result).isEqualTo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)),
                () -> verify(connectionIdentifierAssociation, never()).associate(any(), any()),
                () -> verify(aggregateIdGenerator, never()).generate(eq(TodoId.class), any()),
                () -> verify(aggregateIdGenerator, never()).generate(ArgumentMatchers.<For<TodoId>>any(), any()));
    }

    @Test
    void shouldGenerateAndAssociateAggregateIdWhenAssociationDoesNotExist() throws Exception {
        // Given
        doReturn(Provided.ofUnknown(CONNECTED_USER)).when(connectionAssociationFinder).findByConnectedUser(any());
        doReturn(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)).when(aggregateIdGenerator).generate(TodoId.class, creationalFromSequenceNumber);

        // When
        final TodoId result = connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable,
                creationalFromSequenceNumber);

        // Then
        assertAll(
                () -> verify(connectionIdentifierAssociation, times(1)).associate(CONNECTED_USER, new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)),
                () -> assertThat(result).isEqualTo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)));
    }

    @Test
    void shouldThrowExceptionWhenUnableToFindByHash() throws Exception {
        // Given
        doThrow(new UnableToFindException(new RuntimeException())).when(connectionAssociationFinder).findByConnectedUser(any());

        // When && Then
        assertThatThrownBy(() -> connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable, creationalFromSequenceNumber))
                .isInstanceOf(ConnectedUserAggregateIdProviderException.class)
                .cause()
                .isInstanceOf(UnableToFindException.class);
    }

    @Test
    void shouldThrowExceptionWhenSequenceGenerationFails() throws Exception {
        // given
        doReturn(Provided.ofUnknown(CONNECTED_USER)).when(connectionAssociationFinder).findByConnectedUser(any());
        doThrow(new SequenceGenerationException("fail", "mySequence")).when(aggregateIdGenerator).generate(eq(TodoId.class), any());

        // When && Then
        assertThatThrownBy(() -> connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable, creationalFromSequenceNumber))
                .isInstanceOf(ConnectedUserAggregateIdProviderException.class)
                .cause()
                .isInstanceOf(SequenceGenerationException.class)
                .hasFieldOrPropertyWithValue("sequenceName", "mySequence");
    }

    @Test
    void shouldThrowExceptionWhenAssociationAlreadyAssociated() throws Exception {
        // Given
        doReturn(Provided.ofUnknown(CONNECTED_USER)).when(connectionAssociationFinder).findByConnectedUser(any());
        doReturn(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1)).when(aggregateIdGenerator).generate(eq(TodoId.class), any());

        doThrow(new AlreadyAssociatedException(CONNECTED_USER)).when(connectionIdentifierAssociation).associate(any(), any());

        // When && Then
        assertThatThrownBy(() -> connectedUserAggregateIdProvider.provide(TodoId.class, creationalFromIdentifiable, creationalFromSequenceNumber))
                .isInstanceOf(ConnectedUserAggregateIdProviderException.class)
                .cause()
                .isInstanceOf(AlreadyAssociatedException.class)
                .hasFieldOrPropertyWithValue("connectionIdentifier", CONNECTED_USER);
    }

}
