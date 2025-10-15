package com.damdamdeo.pulse.extension.runtime;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Transaction;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.function.Supplier;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class DefaultQuarkusTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<?>> A joiningExisting(final Supplier<A> callable) {
        return QuarkusTransaction.requiringNew().call(callable::get);
    }
}
