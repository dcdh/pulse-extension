package com.damdamdeo.pulse.extension.hasher.runtime;

import com.damdamdeo.pulse.extension.core.hashing.Hash;
import org.junit.jupiter.api.Test;

import static com.damdamdeo.pulse.extension.hasher.runtime.CustomIdentifiable.GIVEN;
import static org.assertj.core.api.Assertions.assertThat;

class Sha3256DefaultHasherTest {

    Sha3256DefaultHasher sha3256DefaultHasher = new Sha3256DefaultHasher();

    @Test
    void shouldHash() {
        // Given

        // When
        final Hash<CustomIdentifiable> hash = sha3256DefaultHasher.hash(GIVEN);

        // Then
        assertThat(hash).isEqualTo(GIVEN.expected());
    }
}
