package com.damdamdeo.pulse.extension.core.hashing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashTest {

    @Test
    void shouldFailWhenHashIsInvalid() {
        assertThatThrownBy(() -> new Hash<>("boom"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("invalid hash");
    }
}
