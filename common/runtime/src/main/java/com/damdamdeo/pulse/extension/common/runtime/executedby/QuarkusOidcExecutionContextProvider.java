package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.ExecutionContext;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

@ApplicationScoped
@Unremovable
public class QuarkusOidcExecutionContextProvider implements ExecutionContextProvider {

    public static final String USER_ROLE = "user";

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public ExecutionContext provide() {
        final Set<String> roles = securityIdentity.getRoles();
        if (securityIdentity.isAnonymous()) {
            return new ExecutionContext(ExecutedBy.Anonymous.INSTANCE, roles);
        } else if (securityIdentity.getRoles().contains(USER_ROLE)) {
            return new ExecutionContext(new ExecutedBy.EndUser(securityIdentity.getPrincipal().getName(), true), roles);
        } else {
            return new ExecutionContext(new ExecutedBy.ServiceAccount(securityIdentity.getPrincipal().getName()), roles);
        }
    }
}
