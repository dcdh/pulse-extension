package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface MultipleResultProjectionQuery<I extends Input> {
    String query(Passphrase passphrase, OwnedBy ownedBy, I input);
}
