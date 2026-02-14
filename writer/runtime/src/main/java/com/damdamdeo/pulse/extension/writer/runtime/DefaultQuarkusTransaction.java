package com.damdamdeo.pulse.extension.writer.runtime;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.command.BusinessCallable;
import com.damdamdeo.pulse.extension.core.command.Transaction;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
@DefaultBean
public final class DefaultQuarkusTransaction implements Transaction {

    @Override
    public <A extends AggregateRoot<?>> A requiringNew(final BusinessCallable<A> callable) throws BusinessException {
        try {
            return QuarkusTransaction.requiringNew().call(callable::call);
        } catch (final QuarkusTransactionException quarkusTransactionException) {
            if (quarkusTransactionException.getCause() instanceof BusinessException) {
                throw (BusinessException) quarkusTransactionException.getCause();
            } else {
                throw new RuntimeException(quarkusTransactionException.getCause());
            }
        }
    }

    @Override
    public <A extends AggregateRoot<?>> A joiningExisting(final BusinessCallable<A> callable) throws BusinessException {
        try {
            return QuarkusTransaction.joiningExisting().call(callable::call);
        } catch (final QuarkusTransactionException quarkusTransactionException) {
            if (quarkusTransactionException.getCause() instanceof BusinessException) {
                throw (BusinessException) quarkusTransactionException.getCause();
            } else {
                throw new RuntimeException(quarkusTransactionException.getCause());
            }
        }
    }
}
