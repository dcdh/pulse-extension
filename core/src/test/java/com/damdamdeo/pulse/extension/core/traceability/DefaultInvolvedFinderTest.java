package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.connecteduser.UsernameEncoded;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AnyIdentifiable;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoded;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultInvolvedFinderTest {

    private static final String TRACEABILITY_ROLE = "read-traces";
    private static final AnyAggregateId LINK_TO = new AnyAggregateId("aggregate");

    @Mock
    private InvolvedFinderRepository involvedFinderRepository;

    @Mock
    private OwnedByProvider ownedByProvider;

    @Mock
    private UsernameDecoder usernameDecoder;

    @Mock
    private ExecutionContextProvider executionContextProvider;

    @Mock
    private TraceAppender traceAppender;

    private final Identifiable identifiable = new AnyIdentifiable("requested-id");

    private DefaultInvolvedFinder finder;

    @BeforeEach
    void setUp() {
        final TraceabilityFeature feature = new TraceabilityFeature(executionContextProvider, () -> TRACEABILITY_ROLE);
        finder = new DefaultInvolvedFinder(involvedFinderRepository, ownedByProvider, usernameDecoder,
                executionContextProvider, traceAppender, feature);
    }

    @Test
    void shouldReturnAndTraceAggregateInvolvement() throws Exception {
        final Pagination requestedPagination = new Pagination(2, 10);
        final Pagination returnedPagination = new Pagination(2, 10);
        final ExecutedByHashed hash = new ExecutedByHashed("EU:hashed");
        final EncodedDetailedInvolved entry = detail(identifiable, hash);
        final EncodedAggregateIdInvolved encoded = new EncodedAggregateIdInvolved(identifiable,
                new Page<>(List.of(entry), returnedPagination, 25), List.of(entry.encodedNbOfExecutions()));
        final OwnedBy owner = new OwnedBy("entry-owner");
        final Username username = new Username("alice@example.com");
        givenTraceabilityReadRole();
        given(involvedFinderRepository.findBy(identifiable, requestedPagination))
                .willReturn(encoded);
        given(ownedByProvider.provide(identifiable)).willReturn(owner);
        given(usernameDecoder.decode(new UsernameEncoded("encoded"), owner)).willReturn(username);

        final AggregateIdInvolved result = finder.findBy(identifiable, requestedPagination);

        final Actor actor = new Actor(hash, new ExecutedBy.EndUser(username));
        assertThat(result).isEqualTo(new AggregateIdInvolved(identifiable, LINK_TO,
                new Page<>(List.of(decodedDetail(entry, actor)), returnedPagination, 25),
                List.of(counts(actor.executedBy()))));
        verify(traceAppender).append(Map.of(identifiable, LINK_TO));
    }

    @Test
    void shouldDecodePageAndTraceEachIdentifiableOnce() throws Exception {
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed hash = new ExecutedByHashed("EU:hashed");
        final Identifiable secondIdentifiable = mock(Identifiable.class);
        final AnyAggregateId secondLink = new AnyAggregateId("aggregate-2");
        final OwnedBy firstOwner = new OwnedBy("first-owner");
        final OwnedBy secondOwner = new OwnedBy("second-owner");
        final Username alice = new Username("alice@example.com");
        final Username bob = new Username("bob@example.com");
        final EncodedDetailedInvolved first = detail(identifiable, hash);
        final EncodedDetailedInvolved duplicate = detail(identifiable, secondLink, hash);
        final EncodedDetailedInvolved second = detail(secondIdentifiable, secondLink, hash);
        givenTraceabilityReadRole();
        given(involvedFinderRepository.findBy(hash, pagination))
                .willReturn(new EncodedExecutedByInvolved(
                        new Page<>(List.of(first, duplicate, second), pagination, 11)));
        given(ownedByProvider.provide(identifiable)).willReturn(firstOwner);
        given(ownedByProvider.provide(secondIdentifiable)).willReturn(secondOwner);
        given(usernameDecoder.decode(new UsernameEncoded("encoded"), firstOwner)).willReturn(alice);
        given(usernameDecoder.decode(new UsernameEncoded("encoded"), secondOwner)).willReturn(bob);

        final ExecutedByInvolved result = finder.findBy(hash, pagination);

        final Actor aliceActor = new Actor(hash, new ExecutedBy.EndUser(alice));
        final Actor bobActor = new Actor(hash, new ExecutedBy.EndUser(bob));
        assertThat(result).isEqualTo(new ExecutedByInvolved(new Page<>(List.of(
                decodedDetail(first, aliceActor), decodedDetail(duplicate, aliceActor), decodedDetail(second, bobActor)),
                pagination, 11)));
        verify(traceAppender).append(Map.of(identifiable, LINK_TO, secondIdentifiable, secondLink));
    }

    @Test
    void shouldTraceAggregateWhenPageIsEmpty() throws Exception {
        final Pagination pagination = new Pagination(0, 10);
        final EncodedAggregateIdInvolved encoded = aggregateInvolved(new Page<>(List.of(), pagination, 0));
        final AnyAggregateId fallback = new AnyAggregateId("requested-id");
        givenTraceabilityReadRole();
        given(involvedFinderRepository.findBy(identifiable, pagination))
                .willReturn(encoded);

        assertThat(finder.findBy(identifiable, pagination))
                .isEqualTo(new AggregateIdInvolved(identifiable, fallback,
                        new Page<>(List.of(), pagination, 0), List.of()));
        verifyNoInteractions(ownedByProvider, usernameDecoder);
        verify(traceAppender).append(Map.of(identifiable, fallback));
    }

    @Test
    void shouldReturnEmptyPageWhenFindingByExecutedByHashed() throws Exception {
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed hash = new ExecutedByHashed("EU:hashed");
        final Page<EncodedDetailedInvolved> emptyPage = new Page<>(List.of(), pagination, 0);
        givenTraceabilityReadRole();
        given(involvedFinderRepository.findBy(hash, pagination))
                .willReturn(new EncodedExecutedByInvolved(emptyPage));

        assertThat(finder.findBy(hash, pagination)).isEqualTo(new ExecutedByInvolved(
                new Page<>(List.of(), pagination, 0)));
        verifyNoInteractions(ownedByProvider, usernameDecoder);
        verify(traceAppender).append(Map.of());
    }

    @Test
    void shouldRejectFindByAggregateIdWhenTraceabilityReadRoleIsMissing() {
        // given
        final Pagination pagination = new Pagination(0, 10);
        givenNoTraceabilityReadRole();

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(identifiable, pagination))
                        .isInstanceOf(FinderException.class)
                        .hasCauseInstanceOf(UnauthorizedException.class),
                () -> verifyNoInteractions(involvedFinderRepository, traceAppender)
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
                () -> verifyNoInteractions(involvedFinderRepository, traceAppender)
        );
    }

    @Test
    void shouldWrapTraceRepositoryExceptionWhenFindingByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final TraceRepositoryException exception = new TraceRepositoryException(new RuntimeException(
                "Unable to find detailed involved"));
        givenTraceabilityReadRole();
        given(involvedFinderRepository.findBy(identifiable, pagination)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(identifiable, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(involvedFinderRepository).findBy(identifiable, pagination),
                () -> verifyNoInteractions(traceAppender)
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
        given(involvedFinderRepository.findBy(executedByHashed, pagination)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(executedByHashed, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(involvedFinderRepository).findBy(executedByHashed, pagination),
                () -> verifyNoInteractions(traceAppender)
        );
    }

    @Test
    void shouldWrapOwnershipFailureWhenFindingByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final OwnedByProviderException exception = new OwnedByProviderException(
                new RuntimeException("Unable to resolve owner"));
        givenTraceabilityReadRole();
        final EncodedAggregateIdInvolved encoded = aggregateInvolved(new Page<>(
                List.of(detail(identifiable, new ExecutedByHashed("EU:hashed"))), pagination, 1));
        given(involvedFinderRepository.findBy(identifiable, pagination)).willReturn(encoded);
        given(ownedByProvider.provide(identifiable)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(identifiable, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(involvedFinderRepository).findBy(identifiable, pagination),
                () -> verifyNoInteractions(usernameDecoder, traceAppender)
        );
    }

    @Test
    void shouldWrapOwnershipFailureWhenFindingByExecutedByHashed() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final ExecutedByHashed executedByHashed = new ExecutedByHashed("EU:hashed");
        final OwnedByProviderException exception = new OwnedByProviderException(
                new RuntimeException("Unable to resolve owner"));
        givenTraceabilityReadRole();
        given(involvedFinderRepository.findBy(executedByHashed, pagination))
                .willReturn(new EncodedExecutedByInvolved(
                        new Page<>(List.of(detail(identifiable, executedByHashed)), pagination, 1)));
        given(ownedByProvider.provide(identifiable)).willThrow(exception);

        // when / then
        assertAll(
                () -> assertThatThrownBy(() -> finder.findBy(executedByHashed, pagination))
                        .isInstanceOf(FinderException.class)
                        .cause().isSameAs(exception),
                () -> verify(involvedFinderRepository).findBy(executedByHashed, pagination),
                () -> verifyNoInteractions(usernameDecoder, traceAppender)
        );
    }

    @Test
    void shouldWrapTraceAppenderExceptionWhenFindingByAggregateId() throws Exception {
        // given
        final Pagination pagination = new Pagination(0, 10);
        final TraceAppenderException exception = new TraceAppenderException(new RuntimeException("Unable to append trace"));
        givenTraceabilityReadRole();
        given(involvedFinderRepository.findBy(identifiable, pagination))
                .willReturn(aggregateInvolved(new Page<>(List.of(), pagination, 0)));
        doThrow(exception).when(traceAppender).append(Map.of(identifiable, AnyAggregateId.from(identifiable)));

        // when / then
        assertThatThrownBy(() -> finder.findBy(identifiable, pagination))
                .isInstanceOf(FinderException.class)
                .cause().isSameAs(exception);
    }

    private EncodedAggregateIdInvolved aggregateInvolved(final Page<EncodedDetailedInvolved> page) {
        return new EncodedAggregateIdInvolved(identifiable, page, List.of());
    }

    private static EncodedActor encodedActor(final ExecutedByHashed hash) {
        return new EncodedActor(hash, new ExecutedByEncoded("EU:encoded"));
    }

    private static EncodedDetailedInvolved detail(final Identifiable identifiable, final ExecutedByHashed hash) {
        return detail(identifiable, LINK_TO, hash);
    }

    private static EncodedDetailedInvolved detail(final Identifiable identifiable, final AnyAggregateId linkTo,
                                                 final ExecutedByHashed hash) {
        final EncodedActor actor = encodedActor(hash);
        return new EncodedDetailedInvolved(new TraceId(1L), new CorrelationId(2L), identifiable, linkTo,
                actor, null, new AggregateVersion(1), Source.COMMAND,
                ExecutionStatus.SUCCESS, new From("Command"), new ExecutedAt(Instant.EPOCH),
                new EncodedNbOfExecutions(actor, new CommandNbOfTimes(1), new CommandUnauthorizedNbOfTimes(2),
                        new CommandBusinessFailedNbOfTimes(3), new QueryNbOfTimes(4),
                        new QueryUnauthorizedNbOfTimes(5), new TraceabilityNbOfTimes(6)));
    }

    private static DetailedInvolved decodedDetail(final EncodedDetailedInvolved entry, final Actor actor) {
        return new DetailedInvolved(entry.traceId(), entry.correlationId(), actor, entry.eventType(),
                entry.aggregateVersion(), entry.source(), entry.executionStatus(), entry.from(), entry.executedAt(),
                counts(actor.executedBy()));
    }

    private static NbOfExecutions counts(final ExecutedBy executedBy) {
        return new NbOfExecutions(executedBy, new CommandNbOfTimes(1), new CommandUnauthorizedNbOfTimes(2),
                new CommandBusinessFailedNbOfTimes(3), new QueryNbOfTimes(4),
                new QueryUnauthorizedNbOfTimes(5), new TraceabilityNbOfTimes(6));
    }

    private void givenTraceabilityReadRole() {
        given(executionContextProvider.provide())
                .willReturn(new ExecutionContext(ExecutedBy.Anonymous.INSTANCE, Set.of(TRACEABILITY_ROLE)));
    }

    private void givenNoTraceabilityReadRole() {
        given(executionContextProvider.provide())
                .willReturn(new ExecutionContext(ExecutedBy.Anonymous.INSTANCE, Set.of()));
    }
}
