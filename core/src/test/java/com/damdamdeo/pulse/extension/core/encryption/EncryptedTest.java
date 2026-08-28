package com.damdamdeo.pulse.extension.core.encryption;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedTest {

    @Nested
    class ByteArrayEncryptedTest {

        @Test
        void shouldBeEquals() {
            // Given
            final Encrypted<byte[]> given = new Encrypted.ByteArrayEncrypted(new byte[]{1, 2, 3});

            // When
            final Encrypted<byte[]> actual = new Encrypted.ByteArrayEncrypted(new byte[]{1, 2, 3});

            // Then
            assertEquals(given, actual);
        }

    }

}
