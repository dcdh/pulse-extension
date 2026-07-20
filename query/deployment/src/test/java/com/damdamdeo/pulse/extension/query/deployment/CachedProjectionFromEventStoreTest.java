package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.query.runtime.EventCounter;
import com.damdamdeo.pulse.extension.query.runtime.EventCounterException;
import io.quarkus.builder.Version;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertSame;

class CachedProjectionFromEventStoreTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(StubPassphraseProvider.class,
                    TodoProjection.class, TodoChecklistProjection.class))
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-cache", Version.getVersion())
            ));

    @Inject
    @CacheName("projection")
    Cache cache;

    @Inject
    ProjectionFromEventStore<TodoProjection> todoProjectionProjectionFromEventStore;

    @Inject
    StubProjectionFromEventStore stubProjectionFromEventStore;

    @Inject
    StubEventCounter stubEventCounter;

    @AfterEach
    @BeforeEach
    void tearDown() {
        stubProjectionFromEventStore.reset();
        stubEventCounter.reset();
        cache.invalidateAll().await().indefinitely();
    }

    // use @Singleton instead of @ApplicationScoped to be able to affect directly the value using
    // stubProjectionFromEventStore.result
    // @ApplicationScoped use a proxy. Doing the previous affectation without using a setter will not work.
    @Singleton
    @Priority(1)
    @Alternative
    public static class StubProjectionFromEventStore implements ProjectionFromEventStore<TodoProjection> {

        final List<String> called = new ArrayList<>();
        Result<TodoProjection> result;
        Optional<Result<TodoProjection>> optionalResult;

        @Override
        public Result<TodoProjection> getOneByAggregateId(final AggregateId aggregateId,
                                                          final SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException {
            called.add("getOneByAggregateId:" + aggregateId.id());
            return result;
        }

        @Override
        public Optional<Result<TodoProjection>> findOneByAggregateId(final AggregateId aggregateId,
                                                                     final SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException {
            called.add("findOneByAggregateId:" + aggregateId.id());
            return optionalResult;
        }

        @Override
        public <I extends Input> Result<TodoProjection> findAllBy(final OwnedBy ownedBy,
                                                                  final I input,
                                                                  final MultipleResultProjectionQuery<I> multipleResultProjectionQuery) throws ProjectionException {
            called.add("findAllBy:" + ownedBy.id());
            return result;
        }

        public void reset() {
            this.called.clear();
            result = null;
            optionalResult = Optional.empty();
        }

        public List<String> getCalled() {
            return called;
        }
    }

    @Singleton
    @Priority(1)
    @Alternative
    public static class StubEventCounter implements EventCounter {

        final List<String> called = new ArrayList<>();
        Integer aggregateCount = 1;
        Integer ownedByCount = 1;

        @Override
        public Integer byOwnedBy(final OwnedBy ownedBy) throws EventCounterException {
            called.add("byOwnedBy:" + ownedBy.id());
            return ownedByCount;
        }

        @Override
        public Integer byAggregateId(final AggregateId aggregateId) throws EventCounterException {
            called.add("byAggregateId:" + aggregateId.id());
            return aggregateCount;
        }

        public void reset() {
            this.called.clear();
            aggregateCount = 1;
            ownedByCount = 1;
        }

        public List<String> getCalled() {
            return called;
        }
    }

    // getOneByAggregateId

    @Test
    void shouldGetOneByAggregateIdDelegateWhenNotCached() {
        // Given
        final Result<TodoProjection> expected = new Result<>(List.of(), java.util.Set.of());
        stubProjectionFromEventStore.result = expected;


        // When
        final Result<TodoProjection> actual = todoProjectionProjectionFromEventStore.getOneByAggregateId(
                TodoId.USER_1_TODO_1, (passphrase, aggregateId1) -> "");

        // Then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertThat(stubProjectionFromEventStore.getCalled()).containsExactly("getOneByAggregateId:U000001-T000001"),
                () -> assertThat(stubEventCounter.getCalled()).containsExactly("byAggregateId:U000001-T000001")
        );
    }

    @Test
    void shouldGetOneByAggregateIdUseCacheWhenCounterHasNotChanged() {
        // Given
        final AggregateId givenAggregateId = TodoId.USER_1_TODO_1;
        final Result<TodoProjection> expected = new Result<>(List.of(), java.util.Set.of());
        stubProjectionFromEventStore.result = expected;
        todoProjectionProjectionFromEventStore.getOneByAggregateId(givenAggregateId, (passphrase, aggregateId) -> "");
        stubProjectionFromEventStore.reset();
        stubEventCounter.reset();

        // When
        final Result<TodoProjection> actual = todoProjectionProjectionFromEventStore.getOneByAggregateId(
                givenAggregateId, (passphrase, aggregateId) -> "");

        // Then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertThat(stubProjectionFromEventStore.getCalled()).isEmpty(),
                () -> assertThat(stubEventCounter.getCalled()).containsExactly("byAggregateId:U000001-T000001")
        );
    }

    @Test
    void shouldGetOneByAggregateIdReloadWhenCounterHasChanged() {
        // Given
        final AggregateId givenAggregateId = TodoId.USER_1_TODO_1;
        final Result<TodoProjection> first = new Result<>(List.of(), java.util.Set.of());
        final Result<TodoProjection> second = new Result<>(List.of(), java.util.Set.of());
        stubProjectionFromEventStore.result = first;
        todoProjectionProjectionFromEventStore.getOneByAggregateId(givenAggregateId, (passphrase, aggregateId) -> "");
        stubProjectionFromEventStore.reset();
        stubEventCounter.aggregateCount = 2;
        stubProjectionFromEventStore.result = second;

        // When
        final Result<TodoProjection> actual = todoProjectionProjectionFromEventStore.getOneByAggregateId(
                givenAggregateId, (passphrase, aggregateId) -> "");

        // Then
        assertAll(
                () -> assertSame(second, actual),
                () -> assertThat(stubProjectionFromEventStore.getCalled()).containsExactly("getOneByAggregateId:U000001-T000001")
        );
    }

    // findOneByAggregateId

    @Test
    void shouldFindOneByAggregateIdCacheOptionalProjection() throws Exception {
        // Given
        final AggregateId givenAggregateId = TodoId.USER_1_TODO_1;
        final Result<TodoProjection> expected = new Result<>(List.of(), java.util.Set.of());
        stubProjectionFromEventStore.optionalResult = Optional.of(expected);

        // When
        final Optional<Result<TodoProjection>> firstCall = todoProjectionProjectionFromEventStore.findOneByAggregateId(
                givenAggregateId, (passphrase, aggregateId) -> "");

        // Then
        assertAll(
                () -> assertThat(firstCall.isPresent()).isTrue(),
                () -> assertThat(expected).isEqualTo(firstCall.get())
        );

        // Given
        stubProjectionFromEventStore.reset();

        // When
        final Optional<Result<TodoProjection>> secondCall = todoProjectionProjectionFromEventStore.findOneByAggregateId(givenAggregateId,
                (passphrase, aggregateId) -> "");

        // Then
        assertAll(
                () -> assertThat(secondCall.isPresent()).isTrue(),
                () -> assertThat(stubProjectionFromEventStore.getCalled()).isEmpty()
        );
    }

    @Test
    void shouldFindOneByAggregateIdNotCacheEmptyOptional() throws Exception {
        // Given
        final AggregateId givenAggregateId = TodoId.USER_1_TODO_1;
        stubProjectionFromEventStore.optionalResult = Optional.empty();

        todoProjectionProjectionFromEventStore.findOneByAggregateId(givenAggregateId,
                (passphrase, aggregateId) -> "");

        // When
        todoProjectionProjectionFromEventStore.findOneByAggregateId(givenAggregateId,
                (passphrase, aggregateId) -> "");

        // Then
        assertThat(stubProjectionFromEventStore.getCalled().size()).isEqualTo(2);
    }

    // findAllBy

    @Test
    void shouldFindAllByPutInCache() throws Exception {
        // Given
        final OwnedBy givenOwnedBy = OwnedBy.from(TodoId.USER_1_TODO_1);
        final Input givenInput = new Input() {
        };
        final Result<TodoProjection> expected = new Result<>(List.of(), java.util.Set.of());
        stubProjectionFromEventStore.result = expected;
        todoProjectionProjectionFromEventStore.findAllBy(givenOwnedBy, givenInput,
                (passphrase, ownedBy, input) -> "");
        stubProjectionFromEventStore.reset();

        // When
        final Result<TodoProjection> actual = todoProjectionProjectionFromEventStore.findAllBy(
                givenOwnedBy, givenInput, (passphrase, ownedBy, input) -> "");

        // Then
        assertAll(
                () -> assertSame(expected, actual),
                () -> assertThat(stubProjectionFromEventStore.getCalled()).isEmpty()
        );
    }
}
