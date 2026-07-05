package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.CachedPassphraseRepository;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.JdbcPostgresPassphraseRepository;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.VaultPassphraseRepository;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CachedPassphraseRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.arc.exclude-types",
                    JdbcPostgresPassphraseRepository.class.getName() + "," + VaultPassphraseRepository.class.getName());

    @Singleton
    static class StubPassphraseRepository implements PassphraseRepository {

        final List<String> called = new ArrayList<>();
        final Map<OwnedBy, Passphrase> stored = new HashMap<>();

        @Override
        public Optional<Passphrase> findBy(final OwnedBy ownedBy) {
            called.add("retrieve" + ownedBy.id());
            return stored.containsKey(ownedBy) ? Optional.of(stored.get(ownedBy)) : Optional.empty();
        }

        @Override
        public List<RetrievedPassphrase> list(final List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
            called.add("retrieve" + multiples.stream().map(OwnedBy::id).collect(Collectors.joining(",")));
            return multiples.stream().map(ownedBy -> new RetrievedPassphrase(ownedBy, stored.get(ownedBy))).toList();
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            called.add("store" + ownedBy.id() + new String(passphrase.passphrase()));
            stored.put(ownedBy, passphrase);
            return passphrase;
        }

        public void reset() {
            this.called.clear();
            this.stored.clear();
        }

        public List<String> getCalled() {
            return called;
        }
    }

    @Inject
    @CacheName(CachedPassphraseRepository.CACHE_NAME)
    Cache cache;

    @Inject
    StubPassphraseRepository stubPassphraseRepository;

    @Inject
    PassphraseRepository passphraseRepository;

    @BeforeEach
    @AfterEach
    void tearDown() {
        cache.invalidateAll().await().indefinitely();
        stubPassphraseRepository.reset();
    }

    @Test
    void shouldStorePutInCache() throws PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given

        // When
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);

        // Then
        assertAll(
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&", "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&"),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1,
                                PassphraseSample.PASSPHRASE_1))
        );
    }

    @Test
    void shouldFindByPutInCache() throws UnableToRetrievePassphraseException, PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given

        // When
        passphraseRepository.findBy(Todo.OWNED_BY_USER_1);
        passphraseRepository.findBy(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "retrieveU000001"),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1, null))
        );
    }

    @Test
    void shouldFindByAfterStoringPutInCache() throws UnableToRetrievePassphraseException, PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);
        cache.invalidateAll().await().indefinitely();

        // When
        passphraseRepository.findBy(Todo.OWNED_BY_USER_1);
        passphraseRepository.findBy(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&", "retrieveU000001"),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1,
                                PassphraseSample.PASSPHRASE_1))
        );
    }

    @Test
    void shouldListAfterStoringPutInCache() throws UnableToRetrievePassphraseException, PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);
        cache.invalidateAll().await().indefinitely();

        // When
        passphraseRepository.list(List.of(Todo.OWNED_BY_USER_1, Todo.OWNED_BY_USER_2));

        // Then
        assertAll(
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&", "retrieveU000001,U000002"),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1,
                                PassphraseSample.PASSPHRASE_1)),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_2).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_2, null))
        );
    }
}
