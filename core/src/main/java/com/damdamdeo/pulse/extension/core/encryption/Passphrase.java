package com.damdamdeo.pulse.extension.core.encryption;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Objects;

public record Passphrase(char[] passphrase) {

    public Passphrase {
        if (passphrase != null) {
            Validate.isTrue(passphrase.length == 32);
        }
    }

    public static Passphrase ofValid(char[] passphrase) {
        Objects.requireNonNull(passphrase);
        return new Passphrase(passphrase);
    }

    public Passphrase ban() {
        return new Passphrase(null);
    }

    public Status status() {
        if (passphrase != null) {
            return Status.VALID;
        } else {
            return Status.BANNED;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Passphrase that = (Passphrase) o;
        return Objects.deepEquals(passphrase, that.passphrase);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(passphrase);
    }
}
