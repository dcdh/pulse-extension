package com.damdamdeo.pulse.extension.core.executedby;

public final class NotAvailableExecutedByProvider implements ExecutedByProvider {

    @Override
    public ExecutedBy provide() {
        return ExecutedBy.NotAvailable.INSTANCE;
    }
}
