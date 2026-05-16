package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConnectionIdentifierAssociationTest {

    public static final Hash<ConnectionIdentifier> GIVEN_HASH = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");

    @Mock
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @Mock
    Hasher hasher;

    @InjectMocks
    ConnectionIdentifierAssociation connectionIdentifierAssociation;

    @Test
    void shouldAssociateWhenConnectionIdentifierIsNew() throws AlreadyAssociatedException, ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        doReturn(GIVEN_HASH).when(hasher).hash(new UserConnectionIdentifier());

        // When
        connectionIdentifierAssociation.associate(new UserConnectionIdentifier(), new UserAggregateId());

        // Then
        verify(connectionIdentifierRepository).store(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"), new UserAggregateId());
    }

    @Test
    void shouldAssociateThrowAlreadyAssociatedExceptionOnDuplicateConnectionIdentifierException() throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        final UserAggregateId givenUserAggregateId = new UserAggregateId();
        doReturn(GIVEN_HASH).when(hasher).hash(new UserConnectionIdentifier());
        doThrow(DuplicateConnectionIdentifierException.class).when(connectionIdentifierRepository).store(GIVEN_HASH, givenUserAggregateId);

        // When && Then
        assertThatThrownBy(() -> connectionIdentifierAssociation.associate(new UserConnectionIdentifier(), givenUserAggregateId))
                .isInstanceOf(AlreadyAssociatedException.class)
                .hasFieldOrPropertyWithValue("connectionIdentifier", new UserConnectionIdentifier());

    }

    @Test
    void shouldAssociateThrowAlreadyAssociatedExceptionOnConnectionIdentifierRepositoryException() throws DuplicateConnectionIdentifierException, ConnectionIdentifierRepositoryException {
        // Given
        final UserAggregateId givenUserAggregateId = new UserAggregateId();
        doReturn(GIVEN_HASH).when(hasher).hash(new UserConnectionIdentifier());
        doThrow(ConnectionIdentifierRepositoryException.class).when(connectionIdentifierRepository).store(GIVEN_HASH, givenUserAggregateId);

        // When && Then
        assertThatThrownBy(() -> connectionIdentifierAssociation.associate(new UserConnectionIdentifier(), givenUserAggregateId))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(ConnectionIdentifierRepositoryException.class);
    }
}
