package com.damdamdeo.pulse.extension.core.connectionidentifier;

import com.damdamdeo.pulse.extension.core.AggregateId;
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
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @InjectMocks
    ConnectionAssociationFinder connectionAssociationFinder;

    @Test
    void shouldFindByHashReturnFoundAggregateFromIdentifiable() throws UnableToFindByHashException, ConnectionIdentifierRepositoryException {
        // Given
        final UserAggregateId givenUserAggregateId = new UserAggregateId();
        doReturn(Optional.of(givenUserAggregateId)).when(connectionIdentifierRepository).findByHash(GIVEN_HASH);

        // When
        final Optional<AggregateId> byHash = connectionAssociationFinder.findByHash(GIVEN_HASH, _ -> givenUserAggregateId);

        // Then
        assertThat(byHash).isEqualTo(Optional.of(givenUserAggregateId));
    }

    @Test
    void shouldFIndByHashReturnEmptyWhenNoIdentifiableAssociated() throws UnableToFindByHashException, ConnectionIdentifierRepositoryException {
        // Given
        doReturn(Optional.empty()).when(connectionIdentifierRepository).findByHash(GIVEN_HASH);

        // When
        final Optional<AggregateId> aggregate = connectionAssociationFinder.findByHash(GIVEN_HASH, _ -> {
            throw new IllegalStateException("Should not be called");
        });

        // Then
        assertThat(aggregate).isEqualTo(Optional.empty());
    }

    @Test
    void shouldFindByHashThrowUnableToFindByHashExceptionOnConnectionIdentifierRepositoryException() throws ConnectionIdentifierRepositoryException {
        // Given
        doThrow(ConnectionIdentifierRepositoryException.class).when(connectionIdentifierRepository).findByHash(GIVEN_HASH);

        // When && Then
        assertThatThrownBy(() -> connectionAssociationFinder.findByHash(GIVEN_HASH, _ -> {
            throw new IllegalStateException("Should not be called");
        }))
                .isInstanceOf(UnableToFindByHashException.class)
                .hasRootCauseInstanceOf(ConnectionIdentifierRepositoryException.class);
    }
}
