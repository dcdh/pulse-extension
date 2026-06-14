package com.damdamdeo.pulse.extension.obfuscator.runtime.annotation;

import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToDeObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnknownObfuscatedException;
import jakarta.ws.rs.ext.ParamConverter;

import java.util.Objects;

public class DeObfuscatingStringParamConverter implements ParamConverter<String> {

    private final Obfuscator obfuscator;

    public DeObfuscatingStringParamConverter(final Obfuscator obfuscator) {
        this.obfuscator = Objects.requireNonNull(obfuscator);
    }

    @Override
    public String fromString(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return obfuscator.deObfuscate(value);
        } catch (UnableToDeObfuscateException | UnknownObfuscatedException e) {
            throw new IllegalArgumentException("Unable to deobfuscate parameter", e);
        }
    }

    @Override
    public String toString(final String value) {
        return value;
    }
}
