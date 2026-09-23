package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DefaultInvolvedFinderTest {

    @Mock
    private EncodedInvolvedRepository encodedInvolvedRepository;

    @Mock
    private OwnedByProvider ownedByProvider;

    @Mock
    private UsernameDecoder usernameDecoder;

    @Mock
    private ExecutionContextProvider executionContextProvider;

    @Mock
    private OwnedBy ownedBy;

    @Mock
    private Username username;

    @Mock
    private AggregateId aggregateId;

    private DefaultInvolvedFinder finder;

    @BeforeEach
    void setUp() {
        finder = new DefaultInvolvedFinder(encodedInvolvedRepository, ownedByProvider, usernameDecoder, executionContextProvider);
    }

    @Test
    void shouldFindByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("EU:encoded");

        final EncodedInvolved encodedInvolved = new EncodedInvolved(aggregateId,
                new EncodedActor(executedByHashed, executedByEncoded), CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);

        final Page<EncodedInvolved> encodedPage = new Page<>(
                List.of(encodedInvolved),
                pagination,
                1);

        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination)).willReturn(encodedPage);
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy))).willReturn(username);

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertThat(result.content()).hasSize(1);
        final Involved firstInvolved = result.content().getFirst();
        assertAll(
                () -> assertThat(result.pagination()).isSameAs(pagination),
                () -> assertThat(result.totalElements()).isEqualTo(1),
                () -> assertThat(firstInvolved.aggregateId()).isSameAs(aggregateId),
                () -> assertThat(firstInvolved.actor().executedByHashed()).isSameAs(executedByHashed),
                () -> assertThat(firstInvolved.actor().executedBy()).isEqualTo(new ExecutedBy.EndUser(username)),
                () -> verify(encodedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindByExecutedByHashed() throws Exception {
        // given
        final Pagination pagination = new Pagination(1, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("EU:encoded");
        final EncodedInvolved encodedInvolved = new EncodedInvolved(aggregateId,
                new EncodedActor(executedByHashed, executedByEncoded), CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);

        final Page<EncodedInvolved> encodedPage = new Page<>(List.of(encodedInvolved), pagination, 11);
        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(executedByHashed, pagination)).willReturn(encodedPage);
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy))).willReturn(username);

        // when
        final Page<Involved> result = finder.findBy(executedByHashed, pagination);

        // then
        assertThat(result.content()).hasSize(1);
        final Involved firstInvolved = result.content().getFirst();
        assertAll(
                () -> assertThat(result.pagination()).isSameAs(pagination),
                () -> assertThat(result.totalElements()).isEqualTo(11),
                () -> assertThat(firstInvolved.aggregateId()).isSameAs(aggregateId),
                () -> assertThat(firstInvolved.actor().executedByHashed()).isSameAs(executedByHashed),
                () -> assertThat(firstInvolved.actor().executedBy()).isEqualTo(new ExecutedBy.EndUser(username)),
                () -> verify(encodedInvolvedRepository).findBy(executedByHashed, pagination),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindEmptyPage() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination)).willReturn(
                new Page<>(List.of(), pagination, 0));

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content()).isEmpty(),
                () -> assertThat(result.pagination()).isSameAs(pagination),
                () -> assertThat(result.totalElements()).isZero(),
                () -> verify(encodedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldFindAnonymousExecutedBy() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed(ExecutedBy.Anonymous.DISCRIMINANT);
        final EncodedInvolved encodedInvolved = new EncodedInvolved(aggregateId, new EncodedActor(executedByHashed,
                new ExecutedByEncoded(ExecutedBy.Anonymous.DISCRIMINANT)), CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);

        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isSameAs(ExecutedBy.Anonymous.INSTANCE),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindNotAvailableExecutedBy() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed(ExecutedBy.NotAvailable.DISCRIMINANT);
        final EncodedInvolved encodedInvolved = new EncodedInvolved(
                aggregateId, new EncodedActor(executedByHashed, new ExecutedByEncoded(ExecutedBy.NotAvailable.DISCRIMINANT)),
                CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedInvolved), pagination, 1));

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isSameAs(ExecutedBy.NotAvailable.INSTANCE),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindServiceAccountExecutedBy() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final String serviceAccount = "my-service";
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("SA:" + serviceAccount);
        final EncodedInvolved encodedInvolved = new EncodedInvolved(
                aggregateId, new EncodedActor(executedByHashed, new ExecutedByEncoded("SA:" + serviceAccount)),
                CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);
        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isEqualTo(new ExecutedBy.ServiceAccount(serviceAccount)),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindBannedExecutedBy() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed(ExecutedBy.Banned.DISCRIMINANT);
        final EncodedInvolved encodedInvolved = new EncodedInvolved(
                aggregateId, new EncodedActor(executedByHashed, new ExecutedByEncoded(ExecutedBy.Banned.DISCRIMINANT)),
                CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);
        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isSameAs(ExecutedBy.Banned.INSTANCE),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldReturnBannedWhenEndUserCannotBeDecoded() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final EncodedInvolved encodedInvolved = new EncodedInvolved(
                aggregateId, new EncodedActor(executedByHashed, new ExecutedByEncoded("EU:encoded")),
                CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);
        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy)))
                .willThrow(new UnableToDecodeException(new RuntimeException("Unable to decode username")));

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isSameAs(ExecutedBy.Banned.INSTANCE),
                () -> verify(encodedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldRejectFindByAggregateIdWhenTraceabilityReadRoleIsMissing() {
        // given
        final Pagination pagination = new Pagination(0, 10);

        givenNoTraceabilityReadRole();

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                        .isInstanceOf(FinderException.class)
                        .hasCauseInstanceOf(UnauthorizedException.class),
                () -> verifyNoInteractions(encodedInvolvedRepository, ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldRejectFindByExecutedByHashedWhenTraceabilityReadRoleIsMissing() {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        givenNoTraceabilityReadRole();

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(executedByHashed, pagination))
                        .isInstanceOf(FinderException.class)
                        .hasCauseInstanceOf(UnauthorizedException.class),
                () -> verifyNoInteractions(encodedInvolvedRepository, ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldWrapTraceRepositoryExceptionWhenFindingByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final TraceRepositoryException exception = new TraceRepositoryException(new RuntimeException("Unable to find involved"));

        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(encodedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldWrapTraceRepositoryExceptionWhenFindingByExecutedByHashed() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final TraceRepositoryException exception = new TraceRepositoryException(new RuntimeException("Unable to find involved"));

        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(executedByHashed, pagination))
                .willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(executedByHashed, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(encodedInvolvedRepository).findBy(executedByHashed, pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldWrapOwnedByProviderExceptionWhenFindingByExecutedByHashed() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final TraceRepositoryException exception = new TraceRepositoryException(new OwnedByProviderException(
                new RuntimeException("Unable to find involved")));

        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(executedByHashed, pagination)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(executedByHashed, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(encodedInvolvedRepository).findBy(executedByHashed, pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldDecodeEndUserWithOwnedBy() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final EncodedInvolved encodedInvolved = new EncodedInvolved(aggregateId,
                new EncodedActor(executedByHashed, new ExecutedByEncoded("EU:encoded")), CommandNbOfTimes.ONE, QueryNbOfTimes.ONE);

        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy))).willReturn(username);

        // when
        finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> verify(ownedByProvider).provide(aggregateId),
                () -> verify(usernameDecoder).decode(any(), same(ownedBy))
        );
    }

    @Test
    void shouldUsePaginationReturnedByRepository() throws Exception {
        // given
        final Pagination requestedPagination = new Pagination(2, 20);
        final Pagination returnedPagination = new Pagination(2, 20);

        givenTraceabilityReadRole();
        given(encodedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), requestedPagination))
                .willReturn(new Page<>(List.of(), returnedPagination, 100));

        // when
        final Page<Involved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), requestedPagination);

        // then
        assertAll(
                () -> assertThat(result.pagination()).isSameAs(returnedPagination),
                () -> assertThat(result.totalElements()).isEqualTo(100)
        );
    }

    private void givenTraceabilityReadRole() {
        given(executionContextProvider.provide())
                .willReturn(new ExecutionContext(ExecutedBy.Anonymous.INSTANCE, Set.of(Finder.ROLE_TRACEABILITY_READ)));
    }

    private void givenNoTraceabilityReadRole() {
        given(executionContextProvider.provide())
                .willReturn(new ExecutionContext(ExecutedBy.Anonymous.INSTANCE, Set.of()));
    }
}
