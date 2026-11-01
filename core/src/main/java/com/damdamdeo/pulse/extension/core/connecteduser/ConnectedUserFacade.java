package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.connectionidentifier.AnyIdentifiable;

import java.util.Optional;

public interface ConnectedUserFacade {

    Optional<AnyIdentifiable> isRegistered() throws RegistrationCheckerException;

    AnyIdentifiable getConnectedUser() throws ConnectedUserNotRegisteredException, RegistrationCheckerException;
}
