package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.query.Input;

import java.util.List;
import java.util.Set;

public interface TraceAppender {

    <K extends AggregateId> void append(Command<K> command, List<VersionizedEvent<K>> versionizedEvents,
                                        ExecutionStatus executionStatus) throws TraceAppenderException;

    <K extends AggregateId> void append(Input input, Set<K> aggregateIds, ExecutionStatus executionStatus)
            throws TraceAppenderException;
}
