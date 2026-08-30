package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateIdDecomposerTest {

    AggregateIdDecomposer aggregateIdDecomposer = new AggregateIdDecomposer();

    @Test
    void shouldUnCompound() {
        // Given
        final Set<AggregateId> givenAggregateIds = Set.of(TodoChecklistId.USER_1_TODO_1_1);

        // When
        final Set<AggregateId> uncompounded = aggregateIdDecomposer.unCompound(givenAggregateIds);

        // Then
        assertThat(uncompounded).containsExactlyInAnyOrder(AnyAggregateId.from(TodoChecklistId.USER_1_TODO_1_1),
                AnyAggregateId.from(TodoId.USER_1_TODO_1),
                AnyAggregateId.from(UserId.USER_1));
    }
}
