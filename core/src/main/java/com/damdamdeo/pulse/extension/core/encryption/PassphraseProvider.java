package com.damdamdeo.pulse.extension.core.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface PassphraseProvider {

    Passphrase provide(final OwnedBy ownedBy);
}
