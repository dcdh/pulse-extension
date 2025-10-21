package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface PassphraseProvider {

    char[] provide(final OwnedBy ownedBy);
}
