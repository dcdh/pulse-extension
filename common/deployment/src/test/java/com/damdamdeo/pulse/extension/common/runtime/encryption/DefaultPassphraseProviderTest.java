package com.damdamdeo.pulse.extension.common.runtime.encryption;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class DefaultPassphraseProviderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Singleton
    static class StubPassphraseRepository implements PassphraseRepository {

        final AtomicBoolean retrieveCalled = new AtomicBoolean(false);

        @Override
        public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
            retrieveCalled.set(true);
            return Optional.of(PassphraseSample.PASSPHRASE_1);
        }

        @Override
        public List<RetrievedPassphrase> list(List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            return null;
        }

        public void reset() {
            this.retrieveCalled.set(false);
        }
    }

    @Inject
    DefaultPassphraseProvider defaultPassphraseProvider;

    @Inject
    StubPassphraseRepository stubPassphraseRepository;

    @Inject
    @CacheName("passphrase")
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
                () -> assertThat(stubPassphraseRepository.retrieveCalled.get()).isTrue(),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1,
                                PassphraseSample.PASSPHRASE_1))
        );
    }

    @Test
    void shouldReuseCache() throws UnableToProvidePassphraseException {
        // Given
        cache.as(CaffeineCache.class).put(Todo.OWNED_BY_USER_1, CompletableFuture.completedFuture(
                new RetrievedPassphrase(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1)));

        // When
        final Passphrase provided = defaultPassphraseProvider.provide(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(provided.passphrase()).containsExactly(PassphraseSample.PASSPHRASE_1.passphrase()),
                () -> assertThat(stubPassphraseRepository.retrieveCalled.get()).isFalse());
    }
}
