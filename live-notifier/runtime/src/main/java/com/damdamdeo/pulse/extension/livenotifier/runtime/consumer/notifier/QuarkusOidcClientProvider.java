package com.damdamdeo.pulse.extension.livenotifier.runtime.consumer.notifier;

import io.quarkus.arc.Unremovable;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Unremovable
public class QuarkusOidcClientProvider implements ClientProvider {

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public Client provide() {
        return new ConnectedClient(securityIdentity.getPrincipal());
    }
}
