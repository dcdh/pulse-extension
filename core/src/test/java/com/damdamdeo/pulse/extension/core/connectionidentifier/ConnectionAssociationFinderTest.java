package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.connecteduser.*;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociationTest.GIVEN_HASH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ConnectionAssociationFinderTest {

    @Mock
    ConnectedUserProvider connectedUserProvider;

    @Mock
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @Mock
    Hasher hasher;

    @InjectMocks
    ConnectionAssociationFinder connectionAssociationFinder;

    private final static ConnectedUser CONNECTED_USER = new ConnectedUser(new Username("damien.clementdhuart@gmail.com"));

    @Test
    void shouldFindByConnectedUserReturnFoundAggregateFromIdentifiable()
            throws UnableToFindException, ConnectionIdentifierRepositoryException, UsernameNotAMailException, ConnectedIsAnonymousException, ConnectedUserNotAvailableException {
        // Given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        final UserAggregateId givenUserAggregateId = new UserAggregateId();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);
        doReturn(Optional.of(givenUserAggregateId)).when(connectionIdentifierRepository).findByHash(GIVEN_HASH);

        // When
        final Provided<AggregateId> provided = connectionAssociationFinder.findByConnectedUser(_ -> givenUserAggregateId);

        // Then
        assertThat(provided).isEqualTo(Provided.ofKnown(givenUserAggregateId, CONNECTED_USER));
    }

    @Test
    void shouldFIndByConnectedUserReturnEmptyWhenNoIdentifiableAssociated()
            throws UnableToFindException, ConnectionIdentifierRepositoryException, UsernameNotAMailException, ConnectedIsAnonymousException, ConnectedUserNotAvailableException {
        // Given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);
        doReturn(Optional.empty()).when(connectionIdentifierRepository).findByHash(GIVEN_HASH);

        // When
        final Provided<AggregateId> provided = connectionAssociationFinder.findByConnectedUser(_ -> {
            throw new IllegalStateException("Should not be called");
        });

        // Then
        assertThat(provided).isEqualTo(Provided.ofUnknown(CONNECTED_USER));
    }

    @Test
    void shouldFindByHashThrowUnableToFindByConnectedUserExceptionOnConnectionIdentifierRepositoryException()
            throws ConnectionIdentifierRepositoryException, UsernameNotAMailException, ConnectedIsAnonymousException, ConnectedUserNotAvailableException {
        // Given
        doReturn(CONNECTED_USER).when(connectedUserProvider).provide();
        doReturn(GIVEN_HASH).when(hasher).hash(CONNECTED_USER);
        doThrow(ConnectionIdentifierRepositoryException.class).when(connectionIdentifierRepository).findByHash(GIVEN_HASH);

        // When && Then
        assertThatThrownBy(() -> connectionAssociationFinder.findByConnectedUser(_ -> {
            throw new IllegalStateException("Should not be called");
        }))
                .isInstanceOf(UnableToFindException.class)
                .hasRootCauseInstanceOf(ConnectionIdentifierRepositoryException.class);
    }
}
