package com.damdamdeo.pulse.extension.core.encryption;

import java.util.Arrays;

public record Passphrase(char[] passphrase) {
    public int length() {
        return passphrase.length;
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
