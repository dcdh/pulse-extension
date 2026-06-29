package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.TechnicalException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;

import java.util.Optional;

public interface ConnectedUserFacade {

    Optional<Identifiable> isRegistered() throws TechnicalException;
}
