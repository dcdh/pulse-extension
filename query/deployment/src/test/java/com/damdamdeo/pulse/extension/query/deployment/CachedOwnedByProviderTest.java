package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.TodoChecklistProjection;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.OwnedByProvider;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.UnableToProvideOwnedByException;
import io.quarkus.builder.Version;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CachedOwnedByProviderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-cache", Version.getVersion())
            ));

    @Inject
    OwnedByProvider ownedByProvider;

    @Inject
    StubOwnedByProvider stubOwnedByProvider;

    @Inject
    @CacheName("ownedBy")
    Cache cache;

    @AfterEach
    @BeforeEach
    void tearDown() {
        stubOwnedByProvider.reset();
        cache.invalidateAll().await().indefinitely();
    }

    @ApplicationScoped
    @Priority(1)
    @Alternative
    public static class StubOwnedByProvider implements OwnedByProvider {

        final List<String> called = new ArrayList<>();

        @Override
        public OwnedBy getByAggregateId(final AggregateId aggregateId) throws UnableToProvideOwnedByException {
            called.add("getByAggregateId:" + aggregateId.id());
            return OwnedBy.from(aggregateId);
        }

        public void reset() {
            this.called.clear();
        }

        public List<String> getCalled() {
            return called;
        }
    }

    @Test
    void shouldGetByAggregateIdPutInCache() throws UnableToProvideOwnedByException {
        // Given

        // When
        final OwnedBy byAggregateId = ownedByProvider.getByAggregateId(TodoId.USER_1_TODO_1);

        // Then
        assertAll(
                () -> assertThat(byAggregateId).isEqualTo(OwnedBy.from(TodoId.USER_1_TODO_1)),
                () -> assertThat(stubOwnedByProvider.getCalled()).containsExactly("getByAggregateId:U000001-T000001"),
                () -> assertThat(cache.as(CaffeineCache.class).keySet()).containsExactly(TodoId.USER_1_TODO_1),
                () -> assertThat(cache.get(TodoId.USER_1_TODO_1, todoId -> {
                    throw new IllegalStateException("should not be called");
                }).await().indefinitely()).isEqualTo(OwnedBy.from(TodoId.USER_1_TODO_1))
        );
    }

    @Test
    void shouldReuseFromCache() throws UnableToProvideOwnedByException {
        // Given
        ownedByProvider.getByAggregateId(TodoId.USER_1_TODO_1);

        // When
        final OwnedBy byAggregateId = ownedByProvider.getByAggregateId(TodoId.USER_1_TODO_1);

        // Then
        assertAll(
                () -> assertThat(byAggregateId).isEqualTo(OwnedBy.from(TodoId.USER_1_TODO_1)),
                () -> assertThat(stubOwnedByProvider.getCalled()).containsExactly("getByAggregateId:U000001-T000001"),
                () -> assertThat(cache.as(CaffeineCache.class).keySet()).containsExactly(TodoId.USER_1_TODO_1)
        );
    }
}
