package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.UnauthorizedException;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameDecoder;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;

import java.util.*;

public final class DefaultInvolvedFinder implements InvolvedFinder {

    private final InvolvedFinderRepository involvedFinderRepository;
    private final OwnedByProvider ownedByProvider;
    private final UsernameDecoder usernameDecoder;
    private final ExecutionContextProvider executionContextProvider;
    private final TraceAppender traceAppender;
    private final TraceabilityFeature traceabilityFeature;

    public DefaultInvolvedFinder(final InvolvedFinderRepository involvedFinderRepository,
                                 final OwnedByProvider ownedByProvider,
                                 final UsernameDecoder usernameDecoder,
                                 final ExecutionContextProvider executionContextProvider,
                                 final TraceAppender traceAppender,
                                 final TraceabilityFeature traceabilityFeature) {
        this.involvedFinderRepository = Objects.requireNonNull(involvedFinderRepository);
        this.ownedByProvider = Objects.requireNonNull(ownedByProvider);
        this.usernameDecoder = Objects.requireNonNull(usernameDecoder);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.traceAppender = Objects.requireNonNull(traceAppender);
        this.traceabilityFeature = Objects.requireNonNull(traceabilityFeature);
    }

    @Override
    public AggregateIdInvolved findBy(final Identifiable identifiable,
                                      final Pagination pagination) throws FinderException {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(pagination);
        return findWithTrace(() -> {
            final EncodedAggregateIdInvolved aggregate = involvedFinderRepository.findBy(
                    identifiable, pagination);
            final Page<EncodedDetailedInvolved> encodedPage = aggregate.page();
            final List<DetailedInvolved> details = new ArrayList<>(encodedPage.content().size());
            for (final EncodedDetailedInvolved entry : encodedPage.content()) {
                details.add(toDetail(entry, decodeActor(entry.encodedActor(), entry.identifiable())));
            }
            final List<NbOfExecutions> counts = new ArrayList<>(aggregate.encodedNbOfExecutions().size());
            for (final EncodedNbOfExecutions entry : aggregate.encodedNbOfExecutions()) {
                counts.add(toCounts(entry, decodeActor(entry.encodedActor(), aggregate.identifiable()).executedBy()));
            }
            final AnyAggregateId linkTo = encodedPage.content().isEmpty()
                    ? AnyAggregateId.from(aggregate.identifiable())
                    : encodedPage.content().getFirst().linkTo();
            traceAppender.append(Map.of(aggregate.identifiable(), linkTo));
            return new AggregateIdInvolved(aggregate.identifiable(), linkTo,
                    new Page<>(details, encodedPage.pagination(), encodedPage.totalElements()), counts);
        });
    }

    @Override
    public ExecutedByInvolved findBy(final ExecutedByHashed executedByHashed, final Pagination pagination)
            throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
        return findWithTrace(() -> {
            final EncodedExecutedByInvolved involved = involvedFinderRepository.findBy(executedByHashed, pagination);
            final Page<EncodedDetailedInvolved> encodedPage = involved.page();
            final List<DetailedInvolved> details = new ArrayList<>(encodedPage.content().size());
            final Map<Identifiable, AnyAggregateId> targets = new LinkedHashMap<>();
            for (final EncodedDetailedInvolved entry : encodedPage.content()) {
                details.add(toDetail(entry, decodeActor(entry.encodedActor(), entry.identifiable())));
                targets.putIfAbsent(entry.identifiable(), entry.linkTo());
            }
            traceAppender.append(targets);
            return new ExecutedByInvolved(new Page<>(details, encodedPage.pagination(), encodedPage.totalElements()));
        });
    }

    private static DetailedInvolved toDetail(final EncodedDetailedInvolved entry, final Actor actor) {
        return new DetailedInvolved(entry.traceId(), entry.correlationId(), actor, entry.eventType(),
                entry.aggregateVersion(), entry.source(), entry.executionStatus(), entry.from(), entry.executedAt(),
                toCounts(entry.encodedNbOfExecutions(), actor.executedBy()));
    }

    private static NbOfExecutions toCounts(final EncodedNbOfExecutions counts, final ExecutedBy executedBy) {
        return new NbOfExecutions(executedBy, counts.commandNbOfTimes(), counts.commandUnauthorizedNbOfTimes(),
                counts.commandBusinessFailedNbOfTimes(), counts.queryNbOfTimes(), counts.queryUnauthorizedNbOfTimes(),
                counts.traceabilityNbOfTimes());
    }

    private Actor decodeActor(final EncodedActor actor, final Identifiable identifiable) throws OwnedByProviderException {
        return new Actor(actor.executedByHashed(),
                actor.executedByEncoded().to(usernameDecoder, ownedByProvider.provide(identifiable)));
    }

    private <T> T findWithTrace(final TraceableLookup<T> lookup) throws FinderException {
        try {
            if (!traceabilityFeature.isEnabled()) {
                throw new UnauthorizedException();
            }
            return lookup.get();
        } catch (final TraceRepositoryException | OwnedByProviderException | UnauthorizedException
                       | TraceAppenderException exception) {
            throw new FinderException(exception);
        }
    }

    @FunctionalInterface
    private interface TraceableLookup<T> {
        T get() throws TraceRepositoryException, OwnedByProviderException, TraceAppenderException;
    }
}
