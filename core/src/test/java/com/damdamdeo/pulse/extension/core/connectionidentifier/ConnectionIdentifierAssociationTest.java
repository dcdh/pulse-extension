package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import com.damdamdeo.pulse.extension.core.hashing.Hasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionIdentifierAssociationTest {

    @Mock
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @Mock
    Hasher hasher;

    @InjectMocks
    ConnectionIdentifierAssociation connectionIdentifierAssociation;

    record UserConnectionIdentifier() implements ConnectionIdentifier {

        @Override
        public String id() {
            return "damien.clementdhuart@gmail.com";
        }
    }

    record UserAggregateId() implements AggregateId {

        @Override
        public String id() {
            return "U-000001";
        }
    }

    @Test
    void shouldAssociateWhenConnectionIdentifierIsNew() throws AlreadyAssociatedException, ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        doReturn(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000")).when(hasher).hash(new UserConnectionIdentifier());

        // When
        connectionIdentifierAssociation.associate(new UserConnectionIdentifier(), new UserAggregateId());

        // Then
        verify(connectionIdentifierRepository).store(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"), new UserAggregateId());
    }

    @Test
    void shouldAssociateThrowAlreadyAssociatedExceptionOnDuplicateConnectionIdentifierException() throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        final UserAggregateId givenUserAggregateId = new UserAggregateId();
        doReturn(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000")).when(hasher).hash(new UserConnectionIdentifier());
        doThrow(DuplicateConnectionIdentifierException.class).when(connectionIdentifierRepository)
                .store(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"), givenUserAggregateId);

        // When && Then
        assertThatThrownBy(() -> connectionIdentifierAssociation.associate(new UserConnectionIdentifier(), givenUserAggregateId))
                .isInstanceOf(AlreadyAssociatedException.class)
                .hasFieldOrPropertyWithValue("connectionIdentifier", new UserConnectionIdentifier());

    }

    @Test
    void shouldAssociateThrowAlreadyAssociatedExceptionOnConnectionIdentifierRepositoryException() throws DuplicateConnectionIdentifierException, ConnectionIdentifierRepositoryException {
        // Given
        final UserAggregateId givenUserAggregateId = new UserAggregateId();
        doReturn(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000")).when(hasher).hash(new UserConnectionIdentifier());
        doThrow(ConnectionIdentifierRepositoryException.class).when(connectionIdentifierRepository)
                .store(new Hash<>("0000000000000000000000000000000000000000000000000000000000000000"), givenUserAggregateId);

        // When && Then
        assertThatThrownBy(() -> connectionIdentifierAssociation.associate(new UserConnectionIdentifier(), givenUserAggregateId))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(ConnectionIdentifierRepositoryException.class);
    }

    @Test
    void shouldFindByHashReturnFoundAggregateFromIdentifiable() throws UnableToFindByHashException, ConnectionIdentifierRepositoryException {
        // Given
        final UserAggregateId givenUserAggregateId = new UserAggregateId();
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");
        doReturn(Optional.of(givenUserAggregateId)).when(connectionIdentifierRepository).findByHash(givenConnectionIdentifier);

        // When
        final Optional<AggregateId> byHash = connectionIdentifierAssociation.findByHash(givenConnectionIdentifier, _ -> givenUserAggregateId);

        // Then
        assertThat(byHash).isEqualTo(Optional.of(givenUserAggregateId));
    }

    @Test
    void shouldFIndByHashReturnEmptyWhenNoIdentifiableAssociated() throws UnableToFindByHashException, ConnectionIdentifierRepositoryException {
        // Given
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");
        doReturn(Optional.empty()).when(connectionIdentifierRepository).findByHash(givenConnectionIdentifier);

        // When
        final Optional<AggregateId> aggregate = connectionIdentifierAssociation.findByHash(givenConnectionIdentifier, _ -> {
            throw new IllegalStateException("Should not be called");
        });

        // Then
        assertThat(aggregate).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByHashThrowUnableToFindByHashExceptionOnConnectionIdentifierRepositoryException() throws ConnectionIdentifierRepositoryException {
        // Given
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");
        doThrow(ConnectionIdentifierRepositoryException.class).when(connectionIdentifierRepository).findByHash(givenConnectionIdentifier);

        // When && Then
        assertThatThrownBy(() -> connectionIdentifierAssociation.findByHash(givenConnectionIdentifier, _ -> {
            throw new IllegalStateException("Should not be called");
        }))
                .isInstanceOf(UnableToFindByHashException.class)
                .hasRootCauseInstanceOf(ConnectionIdentifierRepositoryException.class);
    }
}
