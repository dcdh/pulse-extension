package com.damdamdeo.pulse.extension.encryption.storage.runtime;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;

public interface PassphraseObfuscator {

    Passphrase obfuscate(Passphrase passphrase);
}
