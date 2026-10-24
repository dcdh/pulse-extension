package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UsernameHasher;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DefaultTraceAppender implements TraceAppender {

    private final ExecutionContextProvider executionContextProvider;
    private final ExecutedAtProvider executedAtProvider;
    private final TraceIdGenerator traceIdGenerator;
    private final CorrelationIdProvider correlationIdProvider;
    private final UsernameHasher usernameHasher;
    private final ExecutedByEncodedProvider executedByEncodedProvider;
    private final TraceRecorderRepository traceRecorderRepository;

    public DefaultTraceAppender(final ExecutionContextProvider executionContextProvider,
                                final ExecutedAtProvider executedAtProvider,
                                final TraceIdGenerator traceIdGenerator,
                                final CorrelationIdProvider correlationIdProvider,
                                final UsernameHasher usernameHasher,
                                final ExecutedByEncodedProvider executedByEncodedProvider,
                                final TraceRecorderRepository traceRecorderRepository) {
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.executedAtProvider = Objects.requireNonNull(executedAtProvider);
        this.traceIdGenerator = Objects.requireNonNull(traceIdGenerator);
        this.correlationIdProvider = Objects.requireNonNull(correlationIdProvider);
        this.usernameHasher = Objects.requireNonNull(usernameHasher);
        this.executedByEncodedProvider = Objects.requireNonNull(executedByEncodedProvider);
        this.traceRecorderRepository = Objects.requireNonNull(traceRecorderRepository);
    }

    @Override
    public <A extends AggregateId> void append(final Command<A> command, final List<VersionizedEvent<A>> versionizedEvents,
                                               final ExecutionStatus executionStatus) throws TraceAppenderException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(versionizedEvents);
        Objects.requireNonNull(executionStatus);
        try {
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final A aggregateId = command.id();
            final List<EncodedTraceAggregateId> encodedTraceAggregateIds = new ArrayList<>(versionizedEvents.isEmpty() ? 1 : versionizedEvents.size());
            for (final VersionizedEvent<A> versionizedEvent : versionizedEvents) {
                final EncodedTraceAggregateId encodedTraceAggregateId = EncodedTraceAggregateId.from(command,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(aggregateId, executedBy),
                        EventType.from(versionizedEvent.event().getClass()),
                        versionizedEvent.version());
                encodedTraceAggregateIds.add(encodedTraceAggregateId);
            }
            if (versionizedEvents.isEmpty()) {
                final EncodedTraceAggregateId encodedTraceAggregateId = EncodedTraceAggregateId.from(command,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(aggregateId, executedBy));
                encodedTraceAggregateIds.add(encodedTraceAggregateId);
            }
            traceRecorderRepository.store(new TraceRecorder(traceIdGenerator.generate(),
                    correlationIdProvider.provide(),
                    executedAtProvider.now(),
                    Source.COMMAND, executionStatus, From.from(command), encodedTraceAggregateIds));
        } catch (final TraceIdGeneratorException | ExecutedByEncoderException | TraceRepositoryException |
                       CorrelationIdProviderException exception) {
            throw new TraceAppenderException(exception);
        }
    }

    @Override
    public <A extends AggregateId, P extends Projection<A>> void append(final Input input, final List<P> projections,
                                                                        final ExecutionStatus executionStatus)
            throws TraceAppenderException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(projections);
        Objects.requireNonNull(executionStatus);
        if (projections.isEmpty()) {
            return;
        }
        try {
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final List<EncodedTraceAggregateId> encodedTraceAggregateIds = new ArrayList<>(projections.size());
            for (final Projection<A> projection : projections) {
                EncodedTraceAggregateId encodedTraceAggregateId = EncodedTraceAggregateId.from(projection,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(projection.id(), executedBy));
                encodedTraceAggregateIds.add(encodedTraceAggregateId);
            }
            traceRecorderRepository.store(new TraceRecorder(traceIdGenerator.generate(),
                    correlationIdProvider.provide(),
                    executedAtProvider.now(),
                    Source.QUERY, executionStatus, From.from(input), encodedTraceAggregateIds));
        } catch (final TraceIdGeneratorException | ExecutedByEncoderException | TraceRepositoryException |
                       CorrelationIdProviderException exception) {
            throw new TraceAppenderException(exception);
        }
    }

    @Override
    public void append(final Set<AggregateId> aggregateIds) throws TraceAppenderException {
        Objects.requireNonNull(aggregateIds);
        if (aggregateIds.isEmpty()) {
            return;
        }
        try {
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final List<EncodedTraceAggregateId> encodedTraceAggregateIds = new ArrayList<>(aggregateIds.size());
            for (final AggregateId aggregateId : aggregateIds) {
                EncodedTraceAggregateId encodedTraceAggregateId = EncodedTraceAggregateId.fromTraceability(aggregateId,
                        executedBy.hash(usernameHasher),
                        executedByEncodedProvider.provide(aggregateId, executedBy));
                encodedTraceAggregateIds.add(encodedTraceAggregateId);
            }
            traceRecorderRepository.store(new TraceRecorder(traceIdGenerator.generate(),
                    correlationIdProvider.provide(),
                    executedAtProvider.now(),
                    Source.TRACEABILITY, ExecutionStatus.SUCCESS, new From("TRACEABILITY"), encodedTraceAggregateIds));
        } catch (final TraceIdGeneratorException | ExecutedByEncoderException | TraceRepositoryException |
                       CorrelationIdProviderException exception) {
            throw new TraceAppenderException(exception);
        }
    }
}
