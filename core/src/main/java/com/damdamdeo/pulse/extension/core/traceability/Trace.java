package com.damdamdeo.pulse.extension.core.traceability;

import java.util.List;
import java.util.Objects;

public record Trace(TraceId traceId, ExecutedAt executedAt, ExecutionStatus executionStatus,
                    List<AccessedAggregate> accessedAggregates) {

    public Trace {
        Objects.requireNonNull(traceId);
        Objects.requireNonNull(executedAt);
        Objects.requireNonNull(executionStatus);
        Objects.requireNonNull(accessedAggregates);
    }
}

// FCK je dois avoir un static qui me prends une Command ou une Query
// FCK mieux que ca je fais une interface Traceable implementé cote query et command !
// FCK il me faut du context
// FCK le status devrait être sauvegardé via l'ordinal pour optimiser
// FCK le executedBy devrait être hashé !!! !!!
// FCK comme le hash est sur 64 bits je dois faire le necessaire et en plus je dois hash uniquement si c'est un EndUser ... NON 64 + : + EU
// FCK lors du rest faire le necessaire pour @Provider ...
