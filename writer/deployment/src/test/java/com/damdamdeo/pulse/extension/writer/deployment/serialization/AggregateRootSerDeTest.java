package com.damdamdeo.pulse.extension.writer.deployment.serialization;

import com.damdamdeo.pulse.extension.common.runtime.serialization.BusinessMapper;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateRootSerDeTest {

    final Logger LOGGER = Logger.getLogger(AggregateRootSerDeTest.class.getName());

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withConfigurationResource("application.properties");

    @Inject
    @BusinessMapper
    ObjectMapper objectMapper;

    // language=json
    private final String TODO = """
            {
              "id": {
                "userId": {
                  "sequence": "000001"
                },
                "sequence": "000001"
              },
              "description": "lorem ipsum",
              "status": "IN_PROGRESS",
              "important": false,
              "nullableField": null,
              "belongsTo": "U000001",
              "ownedBy": "U000001"
            }
            """;

    @Test
    void shouldSerializedTodo() throws JsonProcessingException, JSONException {
        // Given
        final Todo givenTodo = new Todo(
                TodoId.USER_1_TODO_1,
                "lorem ipsum",
                Status.IN_PROGRESS,
                false
        );

        // When
        final String serializedJson = objectMapper.writeValueAsString(givenTodo);

        // Then
        LOGGER.info(serializedJson);
        JSONAssert.assertEquals(TODO, serializedJson, JSONCompareMode.STRICT);
    }

    @Test
    void shouldDeserializeTodo() throws JsonProcessingException {
        // Given

        // When
        final Todo todo = objectMapper.readValue(TODO, Todo.class);

        // Then
        assertThat(todo).isEqualTo(
                new Todo(
                        TodoId.USER_1_TODO_1,
                        "lorem ipsum",
                        Status.IN_PROGRESS,
                        false));
    }
}
