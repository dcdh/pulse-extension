package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Objects;
import java.util.Optional;

public final class AnyEndUser implements Permission {

    public static final AnyEndUser INSTANCE = new AnyEndUser();

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(permissionExecutionContext);
        if (permissionExecutionContext.isEndUser()) {
            return Optional.of(decorated.execute(input));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public int priority() {
        return 1;
    }
}
