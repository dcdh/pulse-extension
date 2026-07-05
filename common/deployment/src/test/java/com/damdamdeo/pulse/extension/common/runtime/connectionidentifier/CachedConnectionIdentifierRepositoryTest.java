package com.damdamdeo.pulse.extension.common.runtime.connectionidentifier;

import com.damdamdeo.pulse.extension.common.runtime.StubPassphraseRepository;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifier;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepositoryException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.DuplicateConnectionIdentifierException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import io.quarkus.builder.Version;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CachedConnectionIdentifierRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubPassphraseRepository.class))
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql", Version.getVersion())
            ));

    @ApplicationScoped
    public static class StubConnectionIdentifierRepository implements ConnectionIdentifierRepository {

        private final Map<ConnectionIdentifier, Identifiable> inMemory = new ConcurrentHashMap<>();
        private final List<String> called = new ArrayList<>();

        @Override
        public ConnectionIdentifier store(final ConnectionIdentifier connectionIdentifier,
                                          final Identifiable identifiable) throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
            Objects.requireNonNull(connectionIdentifier);
            Objects.requireNonNull(identifiable);
            inMemory.put(connectionIdentifier, identifiable);
            called.add("store" + connectionIdentifier.id() + identifiable.id());
            return connectionIdentifier;
        }

        @Override
        public Optional<Identifiable> find(final ConnectionIdentifier connectionIdentifier) throws ConnectionIdentifierRepositoryException {
            Objects.requireNonNull(connectionIdentifier);
            called.add("find" + connectionIdentifier.id());
            return Optional.ofNullable(inMemory.get(connectionIdentifier));
        }

        public void clear() {
            inMemory.clear();
            called.clear();
        }

        public Map<ConnectionIdentifier, Identifiable> getInMemory() {
            return inMemory;
        }

        public List<String> getCalled() {
            return called;
        }
    }

    @Inject
    StubConnectionIdentifierRepository stubConnectionIdentifierRepository;

    @Inject
    ConnectionIdentifierRepository connectionIdentifierRepository;

    @Inject
    @CacheName("connectionIdentifier")
    Cache cache;

    @BeforeEach
    @AfterEach
    void tearDown() {
        cache.invalidateAll().await().indefinitely();
        stubConnectionIdentifierRepository.clear();
    }

    @Test
    void shouldStorePutInCache() throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given

        // When
        connectionIdentifierRepository.store(new ConnectionIdentifier(UserId.USER_1.id()), UserId.USER_1);

        // Then
        assertAll(
                () -> assertThat(cache.as(CaffeineCache.class).keySet()).containsExactly(new ConnectionIdentifier("U000001")),
                () -> assertThat(cache.as(CaffeineCache.class).get(new ConnectionIdentifier("U000001"), (_) -> {
                    throw new IllegalStateException("Should not be called");
                }).await().indefinitely())
                        .isEqualTo(UserId.USER_1));
    }

    @Test
    void shouldFindDoNotStoreInCacheWhenNotFound() throws ConnectionIdentifierRepositoryException {
        // Given

        // When
        final Optional<Identifiable> identifiable = connectionIdentifierRepository.find(new ConnectionIdentifier(UserId.USER_1.id()));

        // Then
        assertAll(
                () -> assertThat(identifiable).isEmpty(),
                () -> assertThat(stubConnectionIdentifierRepository.getCalled()).containsExactly("findU000001"),
                () -> assertThat(cache.as(CaffeineCache.class).keySet()).isEmpty()
        );
    }

    @Test
    void shouldFindUseCacheWhenExists() throws ConnectionIdentifierRepositoryException, DuplicateConnectionIdentifierException {
        // Given
        connectionIdentifierRepository.store(new ConnectionIdentifier(UserId.USER_1.id()), UserId.USER_1);
        cache.invalidateAll().await().indefinitely();

        // When
        final Optional<Identifiable> identifiableCallOne = connectionIdentifierRepository.find(new ConnectionIdentifier(UserId.USER_1.id()));
        final Optional<Identifiable> identifiableCallTwo = connectionIdentifierRepository.find(new ConnectionIdentifier(UserId.USER_1.id()));

        // Then
        assertAll(
                () -> assertThat(identifiableCallOne).contains(UserId.USER_1),
                () -> assertThat(identifiableCallOne).isEqualTo(identifiableCallTwo),
                () -> assertThat(cache.as(CaffeineCache.class).keySet()).containsExactly(new ConnectionIdentifier("U000001")),
                () -> assertThat(cache.as(CaffeineCache.class).get(new ConnectionIdentifier("U000001"), (_) -> {
                    throw new IllegalStateException("Should not be called");
                }).await().indefinitely())
                        .isEqualTo(UserId.USER_1),
                () -> assertThat(stubConnectionIdentifierRepository.getCalled()).containsExactly("storeU000001U000001", "findU000001"));
    }
}
