package com.damdamdeo.pulse.extension.traceability.deployment.finder;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Set;

import static com.damdamdeo.pulse.extension.core.traceability.Finder.ROLE_TRACEABILITY_READ;

@ApplicationScoped
@Priority(1)
@Alternative
public class StubExecutionContextProvider implements ExecutionContextProvider {

    @Override
    public ExecutionContext provide() {
        return new ExecutionContext(new ExecutedBy.EndUser(new Username("alice@mail.com")), Set.of(ROLE_TRACEABILITY_READ));
    }
}
