package com.damdamdeo.pulse.extension.core.query.audience;

import com.damdamdeo.pulse.extension.core.query.*;

import java.util.Optional;

// define a priority to avoid unnecessary computing.
// example: a Query has ROLE_RESTRICTED and PARTICIPANT role. ROLE_RESTRICATED is ultra-fast meanwhile IN_EXECUTED_BY
// will do a lot of processing, so ROLE-based must be checked first.
public sealed interface Audience permits Everyone, RoleRestricted, InExecutedBy {

    <I extends Input, P extends Projection> Optional<Result<P>> execute(I input, QueryUseCase<I, P> decorated,
                                                                        AudienceExecutionContext audienceExecutionContext)
            throws QueryException;

    int priority();
}
