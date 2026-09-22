package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDetailedInvolvedFinderTest {

    @Mock
    private EncodedDetailedInvolvedRepository encodedDetailedInvolvedRepository;

    @Mock
    private OwnedByProvider ownedByProvider;

    @Mock
    private UsernameDecoder usernameDecoder;

    @Mock
    private ExecutionContextProvider executionContextProvider;

    @Mock
    private OwnedBy ownedBy;

    @Mock
    private AggregateId aggregateId;

    @Mock
    private TraceId traceId;

    @Mock
    private Source source;

    @Mock
    private From from;

    @Mock
    private ExecutedAt executedAt;

    @Mock
    private com.damdamdeo.pulse.extension.core.connecteduser.Username username;

    private DefaultDetailedInvolvedFinder finder;

    @BeforeEach
    void setUp() {
        finder = new DefaultDetailedInvolvedFinder(encodedDetailedInvolvedRepository, ownedByProvider, usernameDecoder,
                executionContextProvider);
    }

    @Test
    void shouldFindByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final ExecutedByEncoded executedByEncoded = new ExecutedByEncoded("EU:encoded");
        final EncodedDetailedInvolved encodedDetailedInvolved = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(executedByHashed, executedByEncoded), source, from, executedAt);
        final Page<EncodedDetailedInvolved> encodedPage = new Page<>(List.of(encodedDetailedInvolved), pagination, 1);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination)).willReturn(encodedPage);
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy))).willReturn(username);

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertThat(result.content()).hasSize(1);
        final DetailedInvolved detailedInvolved = result.content().getFirst();
        assertAll(
                () -> assertThat(result.pagination()).isSameAs(pagination),
                () -> assertThat(result.totalElements()).isEqualTo(1),
                () -> assertThat(detailedInvolved.traceId()).isSameAs(traceId),
                () -> assertThat(detailedInvolved.source()).isSameAs(source),
                () -> assertThat(detailedInvolved.from()).isSameAs(from),
                () -> assertThat(detailedInvolved.executedAt()).isSameAs(executedAt),
                () -> assertThat(detailedInvolved.aggregateId()).isSameAs(aggregateId),
                () -> assertThat(detailedInvolved.actor().executedByHashed()).isSameAs(executedByHashed),
                () -> assertThat(detailedInvolved.actor().executedBy()).isEqualTo(new ExecutedBy.EndUser(username)),
                () -> verify(encodedDetailedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindByExecutedByHashed() throws Exception {
        // given
        final Pagination pagination = new Pagination(1, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final EncodedDetailedInvolved encodedDetailedInvolved = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(executedByHashed, new ExecutedByEncoded("EU:encoded")), source, from, executedAt);
        final Page<EncodedDetailedInvolved> encodedPage = new Page<>(List.of(encodedDetailedInvolved), pagination, 11);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(executedByHashed, pagination)).willReturn(encodedPage);
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy))).willReturn(username);

        // when
        final Page<DetailedInvolved> result = finder.findBy(executedByHashed, pagination);

        // then
        assertThat(result.content()).hasSize(1);
        final DetailedInvolved detailedInvolved = result.content().getFirst();
        assertAll(
                () -> assertThat(result.pagination()).isSameAs(pagination),
                () -> assertThat(result.totalElements()).isEqualTo(11),
                () -> assertThat(detailedInvolved.traceId()).isSameAs(traceId),
                () -> assertThat(detailedInvolved.source()).isSameAs(source),
                () -> assertThat(detailedInvolved.from()).isSameAs(from),
                () -> assertThat(detailedInvolved.executedAt()).isSameAs(executedAt),
                () -> assertThat(detailedInvolved.aggregateId()).isSameAs(aggregateId),
                () -> assertThat(detailedInvolved.actor().executedByHashed()).isSameAs(executedByHashed),
                () -> assertThat(detailedInvolved.actor().executedBy()).isEqualTo(new ExecutedBy.EndUser(username)),
                () -> verify(encodedDetailedInvolvedRepository).findBy(executedByHashed, pagination),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindEmptyPage() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(), pagination, 0));

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content()).isEmpty(),
                () -> assertThat(result.pagination()).isSameAs(pagination),
                () -> assertThat(result.totalElements()).isZero(),
                () -> verify(encodedDetailedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldFindAnonymousExecutedBy() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed(ExecutedBy.Anonymous.DISCRIMINANT);
        final EncodedDetailedInvolved encodedDetailedInvolved = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(executedByHashed, new ExecutedByEncoded(ExecutedBy.Anonymous.DISCRIMINANT)),
                source, from, executedAt);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedDetailedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

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
        final EncodedDetailedInvolved encodedDetailedInvolved = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(executedByHashed, new ExecutedByEncoded(ExecutedBy.NotAvailable.DISCRIMINANT)),
                source, from, executedAt);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedDetailedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

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
        final EncodedDetailedInvolved encodedDetailedInvolved = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(executedByHashed, new ExecutedByEncoded("SA:" + serviceAccount)), source, from, executedAt);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedDetailedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isEqualTo(
                        new ExecutedBy.ServiceAccount(serviceAccount)),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldFindBannedExecutedBy() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed(ExecutedBy.Banned.DISCRIMINANT);
        final EncodedDetailedInvolved encodedDetailedInvolved = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(executedByHashed, new ExecutedByEncoded(ExecutedBy.Banned.DISCRIMINANT)), source, from, executedAt);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedDetailedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isSameAs(ExecutedBy.Banned.INSTANCE),
                () -> verify(ownedByProvider).provide(aggregateId)
        );
    }

    @Test
    void shouldReturnBannedWhenEndUserCannotBeDecoded()
            throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final EncodedDetailedInvolved encodedDetailedInvolved = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(executedByHashed, new ExecutedByEncoded("EU:encoded")), source, from, executedAt);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(encodedDetailedInvolved), pagination, 1));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy)))
                .willThrow(new com.damdamdeo.pulse.extension.core.executedby.UnableToDecodeException(
                        new RuntimeException("Unable to decode username")));

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertAll(
                () -> assertThat(result.content().getFirst().actor().executedBy()).isSameAs(ExecutedBy.Banned.INSTANCE),
                () -> assertThat(result.content().getFirst().traceId()).isSameAs(traceId),
                () -> assertThat(result.content().getFirst().source()).isSameAs(source),
                () -> assertThat(result.content().getFirst().from()).isSameAs(from),
                () -> assertThat(result.content().getFirst().executedAt()).isSameAs(executedAt),
                () -> verify(encodedDetailedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
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
                () -> verifyNoInteractions(encodedDetailedInvolvedRepository, ownedByProvider, usernameDecoder)
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
                () -> verifyNoInteractions(encodedDetailedInvolvedRepository, ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldWrapTraceRepositoryExceptionWhenFindingByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final TraceRepositoryException exception = new TraceRepositoryException(new RuntimeException(
                "Unable to find detailed involved"));
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(encodedDetailedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldWrapOwnedByProviderExceptionWhenFindingByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final TraceRepositoryException exception = new TraceRepositoryException(new OwnedByProviderException(
                new RuntimeException("Unable to find detailed involved")));
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(encodedDetailedInvolvedRepository).findBy(aggregateId, new IncludeUncompounded(false), pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldWrapTraceRepositoryExceptionWhenFindingByExecutedByHashed() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final TraceRepositoryException exception = new TraceRepositoryException(new RuntimeException(
                "Unable to find detailed involved"));
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(executedByHashed, pagination)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(executedByHashed, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(encodedDetailedInvolvedRepository).findBy(executedByHashed, pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldWrapOwnedByProviderExceptionWhenFindingByExecutedByHashed() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final TraceRepositoryException exception = new TraceRepositoryException(new OwnedByProviderException(
                new RuntimeException("Unable to find detailed involved")));
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(executedByHashed, pagination)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(executedByHashed, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(encodedDetailedInvolvedRepository).findBy(executedByHashed, pagination),
                () -> verifyNoInteractions(ownedByProvider, usernameDecoder)
        );
    }

    @Test
    void shouldMapAllElements() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final AggregateId secondAggregateId = mock(AggregateId.class);
        final TraceId secondTraceId = mock(TraceId.class);
        final Source secondSource = mock(Source.class);
        final From secondFrom = mock(From.class);
        final ExecutedAt secondExecutedAt = mock(ExecutedAt.class);
        final ExecutedByHashed firstHash = new ExecutedByHashed("EU:first");
        final ExecutedByHashed secondHash = new ExecutedByHashed("SA:second");
        final EncodedDetailedInvolved first = new EncodedDetailedInvolved(traceId, aggregateId,
                new EncodedActor(firstHash, new ExecutedByEncoded("EU:first")), source, from, executedAt);
        final EncodedDetailedInvolved second = new EncodedDetailedInvolved(secondTraceId, secondAggregateId,
                new EncodedActor(secondHash, new ExecutedByEncoded("SA:second")), secondSource, secondFrom, secondExecutedAt);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), pagination))
                .willReturn(new Page<>(List.of(first, second), pagination, 2));
        given(ownedByProvider.provide(aggregateId)).willReturn(ownedBy);
        given(ownedByProvider.provide(secondAggregateId)).willReturn(ownedBy);
        given(usernameDecoder.decode(any(), same(ownedBy))).willReturn(username);

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), pagination);

        // then
        assertThat(result.content()).hasSize(2);
        final DetailedInvolved firstResult = result.content().get(0);
        final DetailedInvolved secondResult = result.content().get(1);

        assertAll(
                () -> assertThat(firstResult.traceId()).isSameAs(traceId),
                () -> assertThat(firstResult.source()).isSameAs(source),
                () -> assertThat(firstResult.from()).isSameAs(from),
                () -> assertThat(firstResult.executedAt()).isSameAs(executedAt),
                () -> assertThat(firstResult.aggregateId()).isSameAs(aggregateId),
                () -> assertThat(firstResult.actor().executedByHashed()).isSameAs(firstHash),
                () -> assertThat(firstResult.actor().executedBy()).isEqualTo(new ExecutedBy.EndUser(username)),
                () -> assertThat(secondResult.traceId()).isSameAs(secondTraceId),
                () -> assertThat(secondResult.source()).isSameAs(secondSource),
                () -> assertThat(secondResult.from()).isSameAs(secondFrom),
                () -> assertThat(secondResult.executedAt()).isSameAs(secondExecutedAt),
                () -> assertThat(secondResult.aggregateId()).isSameAs(secondAggregateId),
                () -> assertThat(secondResult.actor().executedByHashed()).isSameAs(secondHash),
                () -> assertThat(secondResult.actor().executedBy()).isEqualTo(new ExecutedBy.ServiceAccount("second"))
        );
    }

    @Test
    void shouldUseRepositoryPaginationAndTotalElements() throws Exception {
        // given
        final Pagination requestedPagination = new Pagination(2, 20);
        final Pagination returnedPagination = new Pagination(2, 20);
        givenTraceabilityReadRole();
        given(encodedDetailedInvolvedRepository.findBy(aggregateId, new IncludeUncompounded(false), requestedPagination))
                .willReturn(new Page<>(List.of(), returnedPagination, 100));

        // when
        final Page<DetailedInvolved> result = finder.findBy(aggregateId, new IncludeUncompounded(false), requestedPagination);

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
