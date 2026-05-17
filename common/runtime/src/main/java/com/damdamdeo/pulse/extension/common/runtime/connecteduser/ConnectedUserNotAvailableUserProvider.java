package com.damdamdeo.pulse.extension.common.runtime.connecteduser;

import com.damdamdeo.pulse.extension.core.connecteduser.*;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
@DefaultBean
public class ConnectedUserNotAvailableUserProvider implements ConnectedUserProvider {

    @Override
    public ConnectedUser provide() throws ConnectedIsAnonymousException, UsernameNotAMailException, ConnectedUserNotAvailableException {
        throw new ConnectedUserNotAvailableException();
    }
}

