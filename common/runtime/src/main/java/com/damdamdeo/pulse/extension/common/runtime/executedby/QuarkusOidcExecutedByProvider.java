package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByProvider;
import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Unremovable
public class QuarkusOidcExecutedByProvider implements ExecutedByProvider {

    public static final String USER_ROLE = "user";

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public ExecutedBy provide() {
        if (securityIdentity.isAnonymous()) {
            return ExecutedBy.Anonymous.INSTANCE;
        } else if (securityIdentity.getRoles().contains(USER_ROLE)) {
            return new ExecutedBy.EndUser(securityIdentity.getPrincipal().getName(), true);
        } else {
            return new ExecutedBy.ServiceAccount(securityIdentity.getPrincipal().getName());
        }
    }
}
