package com.damdamdeo.pulse.extension.core.connecteduser;

import com.damdamdeo.pulse.extension.core.TechnicalException;

public interface ConnectedUserFacade {

    boolean isRegistered() throws TechnicalException;
}
