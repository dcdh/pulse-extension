package com.damdamdeo.pulse.extension.obfuscator.runtime;

import com.damdamdeo.pulse.extension.core.obfuscator.UnableToDeObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToObfuscateException;

import java.util.Optional;
import java.util.UUID;

public interface ObfuscatorRepository {

    UUID store(UUID obfuscated, String value) throws UnableToObfuscateException;

    Optional<String> retrieve(UUID obfuscated) throws UnableToDeObfuscateException;
}
