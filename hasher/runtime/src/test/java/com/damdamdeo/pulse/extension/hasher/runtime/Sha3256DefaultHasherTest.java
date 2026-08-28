package com.damdamdeo.pulse.extension.hasher.runtime;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.Optional;

import static com.damdamdeo.pulse.extension.hasher.runtime.CustomIdentifiable.GIVEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class Sha3256DefaultHasherTest {

    HasherConfig hasherConfig;
    Sha3256DefaultHasher sha3256DefaultHasher;

    @BeforeEach
    void setUp() {
        hasherConfig = Mockito.mock(HasherConfig.class);
        sha3256DefaultHasher = new Sha3256DefaultHasher(hasherConfig);
        doReturn(Optional.empty()).when(hasherConfig).pepper();
    }

    @AfterEach
    void tearDown() {
        verify(hasherConfig).pepper();
    }

    @Test
    void shouldHash() {
        // Given

        // When
        final String hash = sha3256DefaultHasher.hash(GIVEN.id());

        // Then
        assertThat(hash).isEqualTo(GIVEN.expected().value());
    }

    @Test
    void shouldHashFromIdentifiable() {
        // Given

        // When
        final Hash<CustomIdentifiable> hash = sha3256DefaultHasher.hash(GIVEN);

        // Then
        assertThat(hash).isEqualTo(GIVEN.expected());
    }

    @ParameterizedTest
    @MethodSource("com.damdamdeo.pulse.extension.hasher.runtime.HasherProvider#provideUserHash")
    void shouldComputeUserHash(final OwnedBy givenOwnedBy, final Hash<OwnedBy> expectedHash) {
        // Given

        // When
        Hash<OwnedBy> hash = sha3256DefaultHasher.hash(givenOwnedBy);

        // Then
        Assertions.assertThat(hash).isEqualTo(expectedHash);
    }
}
