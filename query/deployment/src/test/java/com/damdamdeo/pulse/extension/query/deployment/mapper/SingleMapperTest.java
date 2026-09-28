package com.damdamdeo.pulse.extension.query.deployment.mapper;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.Result;
import com.damdamdeo.pulse.extension.query.runtime.mapper.SingleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class SingleMapperTest {

    static ObjectMapper objectMapper = new ObjectMapper();

    private record TodoItem(TodoChecklistId id,
                            String description) implements Projection<TodoChecklistId> {

        public TodoItem {
            Objects.requireNonNull(id);
            Objects.requireNonNull(description);
        }
    }

    @Test
    void shouldDirectMapping() throws IOException {
        // Given
        final String givenTodo =
                // language=json
                """
                        {
                          "id": {
                            "todoId": {
                              "userId": {
                                "sequence": "000001"
                              },
                              "sequence": "000001"
                            },
                            "sequence": "000001"
                          },
                          "description": "IMPORTANT: pulse extension development"
                        }
                        """;
        final SingleMapper<TodoItem> direct = SingleMapper.single(new TypeReference<>() {
        });

        // When
        final TodoItem todoItem = direct.map(givenTodo, objectMapper);

        // Then
        assertThat(todoItem).isEqualTo(new TodoItem(
                TodoChecklistId.USER_1_TODO_1_1, "IMPORTANT: pulse extension development"));
    }

    @Test
    void shouldResultSingle() throws IOException {
        // Given
        final String givenTodo =
                // language=json
                """
                        {
                          "id": {
                            "todoId": {
                              "userId": {
                                "sequence": "000001"
                              },
                              "sequence": "000001"
                            },
                            "sequence": "000001"
                          },
                          "description": "IMPORTANT: pulse extension development"
                        }
                        """;
        final SingleMapper<Result<TodoChecklistId, TodoItem>> resultSingleMapper = SingleMapper.resultSingle(TodoChecklistId.class, new TypeReference<>() {
        });

        // When
        final Result<TodoChecklistId, TodoItem> result = resultSingleMapper.map(givenTodo, objectMapper);

        // Then
        assertThat(result).isEqualTo(
                Result.of(List.of(new TodoItem(
                        TodoChecklistId.USER_1_TODO_1_1, "IMPORTANT: pulse extension development"))));
    }
}
