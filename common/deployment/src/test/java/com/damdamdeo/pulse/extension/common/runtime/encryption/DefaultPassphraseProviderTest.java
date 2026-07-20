package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.encryption.storage.deployment.StubPassphraseRepository;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.CachedPassphraseRepository;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DefaultPassphraseProviderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubPassphraseRepository.class))
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubPassphraseGenerator implements PassphraseGenerator {

        @Override
        public Passphrase generate() {
            return PassphraseSample.PASSPHRASE_1;
        }
    }

    @Inject
    DefaultPassphraseProvider defaultPassphraseProvider;

    @Inject
    PassphraseRepository passphraseRepository;

    @Inject
    StubPassphraseRepository stubPassphraseRepository;

    @Inject
    @CacheName(CachedPassphraseRepository.CACHE_NAME)
    Cache cache;

    @BeforeEach
    void setup() {
        cache.invalidateAll().await().indefinitely();
        stubPassphraseRepository.reset();
    }

    @Test
    void shouldPushInCache() throws UnableToProvidePassphraseException {
        // Given

        // When
        final Passphrase provided = defaultPassphraseProvider.provide(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(provided.passphrase()).containsExactly(PassphraseSample.PASSPHRASE_1.passphrase()),
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "retrieveU000001", "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&"),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1,
                                PassphraseSample.PASSPHRASE_1))
        );
    }

    @Test
    void shouldReuseCache() throws UnableToProvidePassphraseException, PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);

        // When
        final Passphrase provided = defaultPassphraseProvider.provide(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(provided.passphrase()).containsExactly(PassphraseSample.PASSPHRASE_1.passphrase()),
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly("storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&"));
    }

    @Test
    void shouldBanUpdateCache() throws UnableToBanPassphraseException, PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);

        // When
        final Passphrase banned = defaultPassphraseProvider.ban(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(banned).isEqualTo(new Passphrase(null)),
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&", "updateU000001"));
    }
}
