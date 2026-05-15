package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ConnectionAssociationFinderTest {

    @Mock
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @InjectMocks
    ConnectionAssociationFinder connectionAssociationFinder;

    @Test
    void shouldFindByHashReturnFoundAggregateFromIdentifiable() throws UnableToFindByHashException, ConnectionIdentifierRepositoryException {
        // Given
        final ConnectionIdentifierAssociationTest.UserAggregateId givenUserAggregateId = new ConnectionIdentifierAssociationTest.UserAggregateId();
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");
        doReturn(Optional.of(givenUserAggregateId)).when(connectionIdentifierRepository).findByHash(givenConnectionIdentifier);

        // When
        final Optional<AggregateId> byHash = connectionAssociationFinder.findByHash(givenConnectionIdentifier, _ -> givenUserAggregateId);

        // Then
        assertThat(byHash).isEqualTo(Optional.of(givenUserAggregateId));
    }

    @Test
    void shouldFIndByHashReturnEmptyWhenNoIdentifiableAssociated() throws UnableToFindByHashException, ConnectionIdentifierRepositoryException {
        // Given
        final Hash<ConnectionIdentifier> givenConnectionIdentifier = new Hash<>("0000000000000000000000000000000000000000000000000000000000000000");
        doReturn(Optional.empty()).when(connectionIdentifierRepository).findByHash(givenConnectionIdentifier);

        // When
        final Optional<AggregateId> aggregate = connectionAssociationFinder.findByHash(givenConnectionIdentifier, _ -> {
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
        assertThatThrownBy(() -> connectionAssociationFinder.findByHash(givenConnectionIdentifier, _ -> {
            throw new IllegalStateException("Should not be called");
        }))
                .isInstanceOf(UnableToFindByHashException.class)
                .hasRootCauseInstanceOf(ConnectionIdentifierRepositoryException.class);
    }
}
