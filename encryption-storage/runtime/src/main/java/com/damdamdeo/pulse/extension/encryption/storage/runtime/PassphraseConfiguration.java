package com.damdamdeo.pulse.extension.encryption.storage.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "pulse.encryption-storage")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PassphraseConfiguration {

    /**
     * masterKey to encode / decode passphrase
     */
    Optional<String> masterKey();
}
