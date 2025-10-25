package com.damdamdeo.pulse.extension.core.encryption;

public record Passphrase(char[] passphrase) {
    public int length() {
        return passphrase.length;
    }
}
