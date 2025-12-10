package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.TodoItem;
import com.damdamdeo.pulse.extension.core.event.MultipleTodoItemsAdded;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
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

class SerializerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @Inject
    ObjectMapper objectMapper;

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
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "IMPORTANT: pulse extension development"),
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "Implement Projection feature"),
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "Organization vacancies"),
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "Go see family")));

        // When
        final String serialized = objectMapper.writeValueAsString(givenMultipleTodoItemsAdded);

        // Then
        // language=json
        final String expected = """
                {
                  "todoItems": [
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "IMPORTANT: pulse extension development"
                    },
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "Implement Projection feature"
                    },
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "Organization vacancies"
                    },
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "Go see family"
                    }
                  ]
                }
                """;
        JSONAssert.assertEquals(expected, serialized, JSONCompareMode.STRICT);
    }

    @Test
    void shouldDeserializeListType() throws JsonProcessingException {
        // Given
        // language=json
        final String given = """
                {
                  "todoItems": [
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "IMPORTANT: pulse extension development"
                    },
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "Implement Projection feature"
                    },
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "Organization vacancies"
                    },
                    {
                      "id": {
                        "todoId": {
                          "user": "Damien",
                          "sequence": 1
                        },
                        "index": 1
                      },
                      "description": "Go see family"
                    }
                  ]
                }
                """;

        // When
        final MultipleTodoItemsAdded multipleTodoItemsAdded = objectMapper.readValue(given, MultipleTodoItemsAdded.class);

        // Then
        assertThat(multipleTodoItemsAdded).isEqualTo(new MultipleTodoItemsAdded(
                List.of(
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "IMPORTANT: pulse extension development"),
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "Implement Projection feature"),
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "Organization vacancies"),
                        new TodoItem(new TodoChecklistId(new TodoId("Damien", 1L), 1L),
                                "Go see family"))));
    }
}
