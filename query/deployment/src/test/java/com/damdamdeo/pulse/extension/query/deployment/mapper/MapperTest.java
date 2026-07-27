package com.damdamdeo.pulse.extension.query.deployment.mapper;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.query.runtime.AggregateIdDeserializer;
import com.damdamdeo.pulse.extension.query.runtime.mapper.Mapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    static ObjectMapper objectMapper = new ObjectMapper();

    private record TodoItem(@JsonDeserialize(using = AggregateIdDeserializer.class) TodoChecklistId id,
                            String description) implements Projection {

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
        final Mapper<TodoItem> direct = Mapper.single(new TypeReference<>() {
        });

        // When
        final TodoItem todoItem = direct.map(givenTodo, objectMapper);

        // Then
        assertThat(todoItem).isEqualTo(new TodoItem(
                TodoChecklistId.USER_1_TODO_1_1, "IMPORTANT: pulse extension development"));
    }

    @Test
    void shouldDirectMappingList() throws IOException {
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
        final Mapper<List<TodoItem>> direct = Mapper.multiple(new TypeReference<>() {
        });

        // When
        final List<TodoItem> todoItems = direct.map(givenTodos, objectMapper);

        // Then
        assertThat(todoItems).containsExactly(new TodoItem(
                TodoChecklistId.USER_1_TODO_1_1, "IMPORTANT: pulse extension development"));
    }
}
