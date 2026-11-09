package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.Projection;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class NoOpTraceAppender implements TraceAppender {

    @Override
    public <A extends AggregateId> void append(final Command<A> command, final List<VersionizedEvent<A>> versionizedEvents,
                                               final ExecutionStatus executionStatus) throws TraceAppenderException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(versionizedEvents);
        Objects.requireNonNull(executionStatus);
        // no-op
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
        // no-op
    }

    @Override
    public void append(final Map<Identifiable, AnyAggregateId> identifiables) throws TraceAppenderException {
        Objects.requireNonNull(identifiables);
        // no-op
    }
}
