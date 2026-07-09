package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.*;
import io.quarkus.arc.Unremovable;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;

import java.util.List;

@Unremovable
@Priority(1)
@Decorator
public class CdiGuardQueryDecorator<I, P extends Projection, R extends Result<P>> implements Query<I, P, R> {

    private final GuardQuery<I, P, R> delegate;

    public CdiGuardQueryDecorator(final ExecutionContextProvider executionContextProvider,
                                  final BackendUserVisibilityRolesProvider backendUserVisibilityRolesProvider,
                                  final ParticipantsProvider participantsProvider,
                                  @Delegate @Any final Query<I, P, R> decorated) {
        this.delegate = new GuardQuery<>(executionContextProvider, backendUserVisibilityRolesProvider, participantsProvider,
                decorated);
    }

    @Override
    public R execute(final I input) throws QueryException {
        return delegate.execute(input);
    }

    @Override
    public List<Audience> audiences() {
        return delegate.audiences();
    }
}
