package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public record RetrievedPassphrase(OwnedBy ownedBy, Passphrase passphrase) {

    public RetrievedPassphrase {
        Objects.requireNonNull(ownedBy);
    }

    public boolean isPresent() {
        return passphrase != null;
    }
}
