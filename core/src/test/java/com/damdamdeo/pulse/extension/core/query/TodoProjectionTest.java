package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TodoProjectionTest {

    @Test
    void shouldExposeTodoAndPreserveOwnerWhenStatusChanges() {
        final Owner owner = new Owner(UserId.USER_2);
        final TodoProjection projection = new TodoProjection(ProjectionId.from(TodoId.USER_1_TODO_1),
                new Description("description"), Status.IN_PROGRESS, true, List.of(), owner);
        final Todo todo = projection;

        assertThat(todo.todoId()).isEqualTo(TodoId.USER_1_TODO_1);
        assertThat(todo.important()).isTrue();
        assertThat(todo.isInProgress()).isTrue();
        assertThat(todo.owner()).isEqualTo(owner);

        final TodoProjection done = projection.withStatus(Status.DONE);
        assertThat(done.isInProgress()).isFalse();
        assertThat(done.owner()).isEqualTo(owner);
        assertThat(done.id()).isEqualTo(projection.id());
        assertThat(projection.status()).isEqualTo(Status.IN_PROGRESS);
        assertThat(projection.withStatus(Status.ARCHIVED).isInProgress()).isFalse();
    }
}
