package com.damdamdeo.pulse.extension.core.projection;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface MultipleResultAggregateQuery {
    String query(Passphrase passphrase, OwnedBy ownedBy);
}
