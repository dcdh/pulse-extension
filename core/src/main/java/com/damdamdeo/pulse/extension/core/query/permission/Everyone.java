package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Objects;
import java.util.Optional;

public final class Everyone implements Permission {

    public static final Everyone INSTANCE = new Everyone();

    private Everyone() {
    }

    @Override
    public <I extends Input, P extends Projection> Optional<Result<P>> execute(final I input, final QueryUseCase<I, P> decorated,
                                                                               final PermissionExecutionContext permissionExecutionContext) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(decorated);
        Objects.requireNonNull(permissionExecutionContext);
        return Optional.of(decorated.execute(input));
    }

    @Override
    public int priority() {
        return 0;
    }
}
