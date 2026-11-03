package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.connectionidentifier.AnyIdentifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;

import java.util.Optional;

public interface ConnectedUserFacade {

    Optional<AnyIdentifiable> isRegistered() throws RegistrationCheckerException;

    AnyIdentifiable getConnectedUser() throws ConnectedUserNotRegisteredException, ConnectedUserException;

    Optional<AnyIdentifiable> findByExecutedBy(ExecutedBy executedBy) throws ConnectedUserException;
}
