package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.ExecutionContext;

import java.util.Set;

public final class NotAvailableExecutionContextProvider implements ExecutionContextProvider {

    @Override
    public ExecutionContext provide() {
        return new ExecutionContext(ExecutedBy.NotAvailable.INSTANCE, Set.of());
    }
}
