package com.damdamdeo.pulse.extension.core.traceability;

import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface OwnedByProvider {

    OwnedBy provide(Identifiable identifiable) throws OwnedByProviderException;
}
