package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class JvmCommandHandlerRegistry implements CommandHandlerRegistry {

    private final ConcurrentHashMap<AggregateId, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <A extends AggregateRoot<?>> A execute(final AggregateId id, final Supplier<A> commandLogic) {
        final ReentrantLock lock = locks.computeIfAbsent(id, key -> new ReentrantLock());
        lock.lock();
        try {
            return commandLogic.get();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                locks.remove(id, lock);
            }
        }
    }
}
