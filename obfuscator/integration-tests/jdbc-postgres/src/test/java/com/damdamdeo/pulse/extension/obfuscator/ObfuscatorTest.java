package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.core.obfuscator.Obfuscator;
import com.damdamdeo.pulse.extension.core.obfuscator.UnknownObfuscatedException;
import com.damdamdeo.pulse.extension.obfuscator.runtime.ObfuscatorRepository;
import com.damdamdeo.pulse.extension.obfuscator.runtime.UUIDProvider;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@QuarkusTest
class ObfuscatorTest {

    private static final UUID GIVEN_UUID = new UUID(0, 0);

    @Inject
    Obfuscator obfuscator;

    @Inject
    @CacheName("obfuscator")
    Cache cache;

    @InjectMock
    ObfuscatorRepository repository;

    @InjectMock
    UUIDProvider uuidProvider;

    @BeforeEach
    @AfterEach
    void tearDown() {
        cache.invalidateAll().await().indefinitely();
    }

    @Test
    void shouldObfuscateAndDeobfuscate() throws Exception {
        // Given
        when(uuidProvider.provide()).thenReturn(GIVEN_UUID);
        when(repository.store(GIVEN_UUID, "secret")).thenReturn(GIVEN_UUID);
        when(repository.retrieve(GIVEN_UUID)).thenReturn(Optional.of("secret"));

        // When
        final String obfuscated = obfuscator.obfuscate("secret");
        final String value = obfuscator.deObfuscate(obfuscated);

        // Then
        assertAll(
                () -> assertEquals("secret", value),
                () -> verify(repository).store(GIVEN_UUID, "secret")
        );
    }

    @Test
    void shouldUseCacheAfterObfuscate() throws Exception {
        // Given
        when(uuidProvider.provide()).thenReturn(GIVEN_UUID);
        when(repository.store(GIVEN_UUID, "secret")).thenReturn(GIVEN_UUID);
        final String obfuscated = obfuscator.obfuscate("secret");

        // When
        final String value = obfuscator.deObfuscate(obfuscated);

        // Then
        assertAll(
                () -> assertEquals("secret", value),
                () -> verify(repository).store(GIVEN_UUID, "secret"),
                // Le cache ayant été rempli lors du obfuscate(),
                // aucun accès au repository lors du deObfuscate().
                () -> verify(repository, never()).retrieve(any()));
    }

    @Test
    void shouldCacheResultAfterFirstDeobfuscate() throws Exception {
        // Given
        when(repository.retrieve(GIVEN_UUID)).thenReturn(Optional.of("secret"));
        final String obfuscated = GIVEN_UUID.toString();

        // When
        final String firstCall = obfuscator.deObfuscate(obfuscated);
        final String secondCall = obfuscator.deObfuscate(obfuscated);

        // Then
        assertAll(
                () -> assertEquals("secret", firstCall),
                () -> assertEquals("secret", secondCall),
                () -> verify(repository, times(1)).retrieve(GIVEN_UUID));
    }

    @Test
    void shouldThrowUnknownObfuscatedException() throws Exception {
        // Given
        when(repository.retrieve(GIVEN_UUID)).thenReturn(Optional.empty());

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> obfuscator.deObfuscate(GIVEN_UUID.toString()))
                        .isExactlyInstanceOf(UnknownObfuscatedException.class),
                () -> verify(repository).retrieve(GIVEN_UUID));
    }

    @Test
    void shouldNotCacheUnknownObfuscatedException() throws Exception {
        // Given
        when(repository.retrieve(GIVEN_UUID)).thenReturn(Optional.empty());

        // When && Then
        assertAll(
                () -> assertThatThrownBy(() -> obfuscator.deObfuscate(GIVEN_UUID.toString()))
                        .isExactlyInstanceOf(UnknownObfuscatedException.class),
                () -> assertThatThrownBy(() -> obfuscator.deObfuscate(GIVEN_UUID.toString()))
                        .isExactlyInstanceOf(UnknownObfuscatedException.class),
                () -> verify(repository, times(2)).retrieve(GIVEN_UUID));
    }

    @Test
    void shouldFailWhenNotAnUUID() {
        assertThatThrownBy(() -> obfuscator.deObfuscate("not-a-uuid"))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
