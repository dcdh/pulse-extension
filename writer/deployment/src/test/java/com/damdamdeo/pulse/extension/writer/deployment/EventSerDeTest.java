package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.TodoItem;
import com.damdamdeo.pulse.extension.core.event.MultipleTodoItemsAdded;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.TodoMarkedAsDone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventSerDeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    ObjectMapper objectMapper;

    private final String listOfTodos =
            // language=json
            """
                    {
                      "todoItems": [
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
                        },
                        {
                          "id": {
                            "todoId": {
                              "userId": {
                                "sequence": "000001"
                              },
                              "sequence": "000001"
                            },
                            "sequence": "000002"
                          },
                          "description": "Implement Projection feature"
                        },
                        {
                          "id": {
                            "todoId": {
                              "userId": {
                                "sequence": "000001"
                              },
                              "sequence": "000001"
                            },
                            "sequence": "000003"
                          },
                          "description": "Organization vacancies"
                        },
                        {
                          "id": {
                            "todoId": {
                              "userId": {
                                "sequence": "000001"
                              },
                              "sequence": "000001"
                            },
                            "sequence": "000004"
                          },
                          "description": "Go see family"
                        }
                      ]
                    }
                    """;

    @Test
    void shouldSerializeSimpleTypes() throws JsonProcessingException, JSONException {
        // Given
        final NewTodoCreated givenNewTodoCreated = new NewTodoCreated("lorem ipsum");

        // When
        final String serialized = objectMapper.writeValueAsString(givenNewTodoCreated);

        // Then
        // language=json
        final String expected = """
                {
                  "description":"lorem ipsum"
                }
                """;
        JSONAssert.assertEquals(expected, serialized, JSONCompareMode.STRICT);
    }

    @Test
    void shouldDeserializeSimpleTypes() throws JsonProcessingException {
        // Given
        // language=json
        final String given = """
                {
                  "description":"lorem ipsum"
                }
                """;

        // When
        final NewTodoCreated newTodoCreated = objectMapper.readValue(given, NewTodoCreated.class);

        // Then
        assertThat(newTodoCreated).isEqualTo(new NewTodoCreated("lorem ipsum"));
    }

    @Test
    void shouldSerializeListType() throws JsonProcessingException, JSONException {
        // Given
        final MultipleTodoItemsAdded givenMultipleTodoItemsAdded = new MultipleTodoItemsAdded(
                List.of(
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                                "IMPORTANT: pulse extension development"),
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_2),
                                "Implement Projection feature"),
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_3),
                                "Organization vacancies"),
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_4),
                                "Go see family")));

        // When
        final String serialized = objectMapper.writeValueAsString(givenMultipleTodoItemsAdded);

        // Then
        JSONAssert.assertEquals(listOfTodos, serialized, JSONCompareMode.STRICT);
    }

    @Test
    void shouldDeserializeListType() throws JsonProcessingException {
        // Given

        // When
        final MultipleTodoItemsAdded multipleTodoItemsAdded = objectMapper.readValue(listOfTodos, MultipleTodoItemsAdded.class);

        // Then
        assertThat(multipleTodoItemsAdded).isEqualTo(new MultipleTodoItemsAdded(
                List.of(
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_1),
                                "IMPORTANT: pulse extension development"),
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_2),
                                "Implement Projection feature"),
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_3),
                                "Organization vacancies"),
                        new TodoItem(new TodoChecklistId(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), TodoChecklistId.SEQUENCE_NUMBER_4),
                                "Go see family"))));
    }

    @Test
    void shouldSerializeEmptyBean() throws JsonProcessingException, JSONException {
        // Given
        final TodoMarkedAsDone givenTodoMarkedAsDone = new TodoMarkedAsDone();

        // When
        final String serialized = objectMapper.writeValueAsString(givenTodoMarkedAsDone);

        // Then
        // language=json
        final String expected = "{}";
        JSONAssert.assertEquals(expected, serialized, JSONCompareMode.STRICT);
    }

    @Test
    void shouldDeserializeEmptyBean() throws JsonProcessingException {
        // Given
        // language=json
        final String given = "{}";

        // When
        final TodoMarkedAsDone todoMarkedAsDone = objectMapper.readValue(given, TodoMarkedAsDone.class);

        // Then
        assertThat(todoMarkedAsDone).isEqualTo(new TodoMarkedAsDone());
    }
}
