package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
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

class SerializationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.arc.exclude-types", PostgresqlSchemaInitializer.class.getName())
            .withConfigurationResource("application.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void shouldSerializedTodo() throws JsonProcessingException, JSONException {
        // Given
        final Todo givenTodo = new Todo(
                new TodoId("Damien", 0L),
                "lorem ipsum",
                Status.IN_PROGRESS,
                false
        );

        // When
        final String serializedJson = objectMapper.writeValueAsString(givenTodo);

        // Then
        JSONAssert.assertEquals(
                // language=json
                """
                        {
                          "id": {
                            "user": "Damien",
                            "sequence": 0
                          },
                          "description":"lorem ipsum",
                          "status":"IN_PROGRESS",
                          "important":false
                        }
                        """, serializedJson, JSONCompareMode.STRICT);
    }
}
