package com.damdamdeo.pulse.extension.common.runtime.connecteduser;

import com.damdamdeo.pulse.extension.core.connecteduser.*;
import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Unremovable
public class QuarkusOidcConnectedUserProvider implements ConnectedUserProvider {

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public ConnectedUser provide() throws ConnectedIsAnonymousException, UsernameNotAMailException, ConnectedUserNotAvailableException {
        if (securityIdentity.isAnonymous()) {
            throw new ConnectedIsAnonymousException();
        } else {
            final String name = securityIdentity.getPrincipal().getName();
            if (!Username.matchEmailPattern(name)) {
                throw new UsernameNotAMailException();
            } else {
                return new ConnectedUser(new Username(name));
            }
        }
    }
}
