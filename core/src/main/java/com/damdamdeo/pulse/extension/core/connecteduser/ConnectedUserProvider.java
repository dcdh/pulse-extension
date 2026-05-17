package com.damdamdeo.pulse.extension.core.connecteduser;

public interface ConnectedUserProvider {

    ConnectedUser provide() throws ConnectedIsAnonymousException, UsernameNotAMailException, ConnectedUserNotAvailableException;
}
