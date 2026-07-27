package com.damdamdeo.pulse.extension.query.deployment.mapper;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.Result;
import com.damdamdeo.pulse.extension.query.runtime.AggregateIdDeserializer;
import com.damdamdeo.pulse.extension.query.runtime.mapper.Mapper;
import com.damdamdeo.pulse.extension.query.runtime.mapper.ResultMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultMapperTest {

    static ObjectMapper objectMapper = new ObjectMapper();

    private record TodoItem(@JsonDeserialize(using = AggregateIdDeserializer.class) TodoChecklistId id,
                            String description) implements Projection {

        public TodoItem {
            Objects.requireNonNull(id);
            Objects.requireNonNull(description);
        }
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
        final Mapper<Result<TodoItem>> resultMapper = ResultMapper.resultSingle(new TypeReference<>() {
        });

        // When
        final Result<TodoItem> result = resultMapper.map(givenTodo, objectMapper);

        // Then
        assertThat(result).isEqualTo(
                new Result<>(List.of(new TodoItem(
                        TodoChecklistId.USER_1_TODO_1_1, "IMPORTANT: pulse extension development")),
                        Set.of(TodoChecklistId.USER_1_TODO_1_1)));
    }

    @Test
    void shouldResultMultiple() throws IOException {
        // Given
        final String givenTodos =
                // language=json
                """
                        [
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
                        ]
                        """;
        final Mapper<Result<TodoItem>> resultMapper = ResultMapper.resultMultiple(new TypeReference<>() {
        });

        // When
        final Result<TodoItem> result = resultMapper.map(givenTodos, objectMapper);

        // Then
        assertThat(result).isEqualTo(
                new Result<>(List.of(new TodoItem(
                        TodoChecklistId.USER_1_TODO_1_1, "IMPORTANT: pulse extension development")),
                        Set.of(TodoChecklistId.USER_1_TODO_1_1)));
    }
}
