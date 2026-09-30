package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;

public interface DistributedLockManager {

    <K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> A executeWithLock(C command,
                                                                                                UseCaseExecutor<K, C, A> useCaseExecutor)
            throws LockingException, UseCaseException;
}
