package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.CachedPassphraseRepository;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.JdbcPostgresPassphraseRepository;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.VaultPassphraseRepository;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CachedPassphraseRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubPassphraseRepository.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.arc.exclude-types",
                    JdbcPostgresPassphraseRepository.class.getName() + "," + VaultPassphraseRepository.class.getName());

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
    void shouldUpdatePutInCache() throws UnableToStorePassphraseException, UnknownPassphraseException, PassphraseAlreadyExistsException {
        // Given
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);
        cache.invalidateAll().await().indefinitely();

        // When
        passphraseRepository.update(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);

        // Then
        assertAll(
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&", "updateU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&"),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1,
                                PassphraseSample.PASSPHRASE_1))
        );
    }

    @Test
    void shouldFindByPutInCache() throws UnableToRetrievePassphraseException {
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
    void shouldGetPutInCache() throws UnableToRetrievePassphraseException, UnknownPassphraseException, PassphraseAlreadyExistsException, UnableToStorePassphraseException {
        // Given
        passphraseRepository.store(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1);
        cache.invalidateAll().await().indefinitely();

        // When
        passphraseRepository.get(Todo.OWNED_BY_USER_1);
        passphraseRepository.get(Todo.OWNED_BY_USER_1);

        // Then
        assertAll(
                () -> assertThat(stubPassphraseRepository.getCalled()).containsExactly(
                        "storeU0000017-YP@28iVU(_#@S%tMrOG6RLQ07ilj&&", "getU000001"),
                () -> assertThat(cache.as(CaffeineCache.class).getIfPresent(Todo.OWNED_BY_USER_1).get())
                        .isEqualTo(new RetrievedPassphrase(Todo.OWNED_BY_USER_1, PassphraseSample.PASSPHRASE_1))
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
