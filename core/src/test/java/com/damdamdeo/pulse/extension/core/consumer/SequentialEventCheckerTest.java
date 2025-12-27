package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.CurrentVersionInConsumption;
import com.damdamdeo.pulse.extension.core.consumer.idempotency.LastConsumedAggregateVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SequentialEventCheckerTest {

    private SequentialEventChecker sequentialEventChecker;

    @BeforeEach
    void setUp() {
        sequentialEventChecker = new SequentialEventChecker();
    }

    @Test
    void shouldReturnOkWhenLastConsumedEventIsCurrentVersionInConsumption() {
        // Given

        // When
        sequentialEventChecker.check(new LastConsumedAggregateVersion(10), new CurrentVersionInConsumption(10));

        // Then
    }

    @Test
    void shouldReturnOkWhenLastConsumedEventIsBeforeOneCurrentVersionInConsumption() {
        // Given

        // When
        sequentialEventChecker.check(new LastConsumedAggregateVersion(9), new CurrentVersionInConsumption(10));

        // Then
    }

    @Test
    void shouldFailWhenLastConsumedEventIsBeforeMoreThanOneCurrentVersionInConsumption() {
        // Given

        // When && Then
        assertThatThrownBy(() -> sequentialEventChecker.check(new LastConsumedAggregateVersion(7), new CurrentVersionInConsumption(10)))
                .isExactlyInstanceOf(SequenceNotRespectedException.class)
                .hasFieldOrPropertyWithValue("missingAggregateVersions", List.of(
                        new AggregateVersion(8), new AggregateVersion(9)));
    }

    @Test
    void shouldReturnOkWhenLastConsumedEventIsAfterCurrentVersionInConsumptionDueToAReplay() {
        // Given

        // When
        sequentialEventChecker.check(new LastConsumedAggregateVersion(9), new CurrentVersionInConsumption(6));

        // Then
    }
}