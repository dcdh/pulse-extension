package com.damdamdeo.pulse.extension.core.encryption;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;

public record Passphrase(char[] passphrase) {

    public Passphrase {
        Validate.isTrue(passphrase.length == 32);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Passphrase that = (Passphrase) o;
        return Arrays.equals(passphrase, that.passphrase);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(passphrase);
    }
}
