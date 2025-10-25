package com.damdamdeo.pulse.extension.runtime.encryption;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DefaultPassphraseGeneratorTest {

    DefaultPassphraseGenerator passphraseGenerator;

    @BeforeEach
    public void setUp() {
        passphraseGenerator = new DefaultPassphraseGenerator();
    }

    @Test
    void shouldGenerate32CharactersPassphrase() {
        // Given

        // When
        final Passphrase generated = passphraseGenerator.generate();
        System.out.println(new String(generated.passphrase()));
        // sample generated 7-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&

        // Then
        assertThat(generated.length()).isEqualTo(32);
    }
}
