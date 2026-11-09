package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameHasher;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultTraceAppender implements TraceAppender {

    private final ExecutionContextProvider executionContextProvider;
    private final ExecutedAtProvider executedAtProvider;
    private final TraceIdGenerator traceIdGenerator;
    private final CorrelationIdProvider correlationIdProvider;
    private final UsernameHasher usernameHasher;
    private final ExecutedByEncodedProvider executedByEncodedProvider;
    private final TraceRecorderRepositoryProvider traceRecorderRepositoryProvider;

    public DefaultTraceAppender(final ExecutionContextProvider executionContextProvider,
                                final ExecutedAtProvider executedAtProvider,
                                final TraceIdGenerator traceIdGenerator,
                                final CorrelationIdProvider correlationIdProvider,
                                final UsernameHasher usernameHasher,
                                final ExecutedByEncodedProvider executedByEncodedProvider,
                                final TraceRecorderRepositoryProvider traceRecorderRepositoryProvider) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.executedAtProvider = Objects.requireNonNull(executedAtProvider);
        this.traceIdGenerator = Objects.requireNonNull(traceIdGenerator);
        this.correlationIdProvider = Objects.requireNonNull(correlationIdProvider);
        this.usernameHasher = Objects.requireNonNull(usernameHasher);
        this.executedByEncodedProvider = Objects.requireNonNull(executedByEncodedProvider);
        this.traceRecorderRepositoryProvider = Objects.requireNonNull(traceRecorderRepositoryProvider);
    }

    @Override
    public <A extends AggregateId> void append(final Command<A> command, final List<VersionizedEvent<A>> versionizedEvents,
                                               final ExecutionStatus executionStatus) throws TraceAppenderException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(versionizedEvents);
        Objects.requireNonNull(executionStatus);
        try {
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final A aggregateId = command.id().linkTo();
            final List<EncodedTraceIdentifiable> encodedTraceIdentifiables = new ArrayList<>(versionizedEvents.isEmpty() ? 1 : versionizedEvents.size());
            for (final VersionizedEvent<A> versionizedEvent : versionizedEvents) {
                final EncodedTraceIdentifiable encodedTraceIdentifiable = EncodedTraceIdentifiable.from(command,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(aggregateId, executedBy),
                        EventType.from(versionizedEvent.event().getClass()),
                        versionizedEvent.version());
                encodedTraceIdentifiables.add(encodedTraceIdentifiable);
            }
            if (versionizedEvents.isEmpty()) {
                final EncodedTraceIdentifiable encodedTraceIdentifiable = EncodedTraceIdentifiable.from(command,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(aggregateId, executedBy));
                encodedTraceIdentifiables.add(encodedTraceIdentifiable);
            }
            final TraceRecorderRepository traceRecorderRepository = traceRecorderRepositoryProvider
                    .provide(TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY);
            traceRecorderRepository.store(new TraceRecorder(traceIdGenerator.generate(),
                    correlationIdProvider.provide(),
                    executedAtProvider.now(),
                    Source.COMMAND, executionStatus, From.from(command), encodedTraceIdentifiables));
        } catch (final TraceIdGeneratorException | ExecutedByEncoderException | TraceRepositoryException |
                       CorrelationIdProviderException exception) {
            throw new TraceAppenderException(exception);
        }
    }

    @Override
    public <A extends AggregateId, P extends Projection<A>> void append(final Input input, final List<P> projections,
                                                                        final ExecutionStatus executionStatus,
                                                                        final TraceabilityDetails traceabilityDetails)
            throws TraceAppenderException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(projections);
        Objects.requireNonNull(executionStatus);
        Objects.requireNonNull(traceabilityDetails);
        if (projections.isEmpty()) {
            return;
        }
        try {
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final List<EncodedTraceIdentifiable> encodedTraceIdentifiables = new ArrayList<>(projections.size());
            for (final Projection<A> projection : projections) {
                EncodedTraceIdentifiable encodedTraceAggregateId = EncodedTraceIdentifiable.from(projection,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(projection.id().linkTo(), executedBy));
                encodedTraceIdentifiables.add(encodedTraceAggregateId);
            }
            final TraceRecorderRepository traceRecorderRepository = traceRecorderRepositoryProvider
                    .provide(TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY);
            traceRecorderRepository.store(new TraceRecorder(traceIdGenerator.generate(),
                    correlationIdProvider.provide(),
                    executedAtProvider.now(),
                    Source.QUERY,
                    executionStatus, From.from(input), encodedTraceIdentifiables));
        } catch (final TraceIdGeneratorException | ExecutedByEncoderException | TraceRepositoryException |
                       CorrelationIdProviderException exception) {
            throw new TraceAppenderException(exception);
        }
    }

    @Override
    public void append(final Map<Identifiable, AnyAggregateId> identifiables) throws TraceAppenderException {
        Objects.requireNonNull(identifiables);
        if (identifiables.isEmpty()) {
            return;
        }
        try {
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final List<EncodedTraceIdentifiable> encodedTraceIdentifiables = new ArrayList<>(identifiables.size());
            for (final Map.Entry<Identifiable, AnyAggregateId> entry : identifiables.entrySet()) {
                final Identifiable identifiable = entry.getKey();
                final AnyAggregateId anyAggregateId = entry.getValue();
                EncodedTraceIdentifiable encodedTraceAggregateId = EncodedTraceIdentifiable.fromTraceability(
                        identifiable,
                        anyAggregateId,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(identifiable, executedBy));
                encodedTraceIdentifiables.add(encodedTraceAggregateId);
            }
            final TraceRecorderRepository traceRecorderRepository = traceRecorderRepositoryProvider
                    .provide(TraceabilityDetails.DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY);
            traceRecorderRepository.store(new TraceRecorder(traceIdGenerator.generate(),
                    correlationIdProvider.provide(),
                    executedAtProvider.now(),
                    Source.TRACEABILITY,
                    ExecutionStatus.SUCCESS, new From("TRACEABILITY"), encodedTraceIdentifiables));
        } catch (final TraceIdGeneratorException | ExecutedByEncoderException | TraceRepositoryException |
                       CorrelationIdProviderException exception) {
            throw new TraceAppenderException(exception);
        }
    }
}
