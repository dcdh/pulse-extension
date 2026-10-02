package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.Handled;

public interface DistributedLockManager {

    <K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> Handled<A, K> executeWithLock(C command,
                                                                                                            UseCaseExecutor<K, C, A> useCaseExecutor)
            throws LockingException, UseCaseException;
}
