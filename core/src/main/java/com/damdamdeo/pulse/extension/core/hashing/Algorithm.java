package com.damdamdeo.pulse.extension.core.hashing;

import java.util.regex.Pattern;

public enum Algorithm {

    SHA3_256 {
        @Override
        public Pattern validationPattern() {
            return Pattern.compile("^[a-z0-9]{64}$");
        }
    };

    public abstract Pattern validationPattern();
}
