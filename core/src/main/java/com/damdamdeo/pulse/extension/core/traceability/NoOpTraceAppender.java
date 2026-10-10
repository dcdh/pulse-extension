package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.query.Input;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class NoOpTraceAppender implements TraceAppender {

    @Override
    public <K extends AggregateId> void append(final Command<K> command, final List<VersionizedEvent<K>> versionizedEvents,
                                               final ExecutionStatus executionStatus) throws TraceAppenderException {
        Objects.requireNonNull(command);
        Objects.requireNonNull(versionizedEvents);
        Objects.requireNonNull(executionStatus);
        // no-op
    }

    @Override
    public <K extends AggregateId> void append(final Input input, final Set<K> aggregateIds, final ExecutionStatus executionStatus)
            throws TraceAppenderException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(aggregateIds);
        Objects.requireNonNull(executionStatus);
        // no-op
    }

    @Override
    public void append(final Set<AggregateId> aggregateIds) throws TraceAppenderException {
        Objects.requireNonNull(aggregateIds);
        // no-op
    }
}
