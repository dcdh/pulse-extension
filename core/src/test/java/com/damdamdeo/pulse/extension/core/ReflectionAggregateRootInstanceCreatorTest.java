package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectionAggregateRootInstanceCreatorTest {

    private final ReflectionAggregateRootInstanceCreator creator = new ReflectionAggregateRootInstanceCreator();

    private final TestId id = new TestId("client-1");

    @Test
    void shouldCreateFullStateAggregateBeforeReplay() {
        // Given
        final Class<FullStateAggregate> aggregateClass = FullStateAggregate.class;

        // When
        final FullStateAggregate aggregate = creator.create(aggregateClass, TestId.class, id);

        // Then
        assertAll(
                () -> assertThat(aggregate.id()).isEqualTo(id),
                () -> assertThat(aggregate.name).isNull(),
                () -> assertThat(aggregate.deleted).isNull()
        );
    }

    @Test
    void shouldKeepLegacyIdOnlyConstructionCompatible() {
        // Given
        final Class<LegacyAggregate> aggregateClass = LegacyAggregate.class;

        // When
        final LegacyAggregate aggregate = creator.create(aggregateClass, TestId.class, id);

        // Then
        assertAll(() -> assertThat(aggregate.id()).isEqualTo(id));
    }

    @Test
    void shouldRejectAmbiguousReplayConstructors() {
        // Given
        final Class<AmbiguousAggregate> aggregateClass = AmbiguousAggregate.class;

        // When
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> creator.create(aggregateClass, TestId.class, id));

        // Then
        assertAll(() -> assertThat(failure).hasMessageContaining("single full-state constructor"));
    }

    @Test
    void shouldRejectNullId() {
        // Given
        final TestId missingId = null;

        // When
        final NullPointerException failure = assertThrows(NullPointerException.class,
                () -> creator.create(FullStateAggregate.class, TestId.class, missingId));

        // Then
        assertAll(() -> assertThat(failure).isInstanceOf(NullPointerException.class));
    }

    public record TestId(String id) implements AggregateId {
    }

    public static final class FullStateAggregate extends AggregateRoot<TestId> {

        private final String name;

        private final Boolean deleted;

        public FullStateAggregate(final TestId id, final String name, final Boolean deleted) {
            super(id);
            this.name = name;
            this.deleted = deleted;
        }

        @Override
        public BelongsTo belongsTo() {
            return BelongsTo.himself(this);
        }

        @Override
        public OwnedBy ownedBy() {
            return OwnedBy.from(id);
        }
    }

    public static final class LegacyAggregate extends AggregateRoot<TestId> {

        public LegacyAggregate(final TestId id) {
            super(id);
        }

        @Override
        public BelongsTo belongsTo() {
            return BelongsTo.himself(this);
        }

        @Override
        public OwnedBy ownedBy() {
            return OwnedBy.from(id);
        }
    }

    public static final class AmbiguousAggregate extends AggregateRoot<TestId> {

        public AmbiguousAggregate(final TestId id, final String name) {
            super(id);
        }

        public AmbiguousAggregate(final TestId id, final String name, final Boolean deleted) {
            super(id);
        }

        @Override
        public BelongsTo belongsTo() {
            return BelongsTo.himself(this);
        }

        @Override
        public OwnedBy ownedBy() {
            return OwnedBy.from(id);
        }
    }
}
