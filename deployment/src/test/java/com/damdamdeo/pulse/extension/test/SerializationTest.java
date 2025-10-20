package com.damdamdeo.pulse.extension.test;

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

import java.util.UUID;

public class SerializationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    ObjectMapper objectMapper;

    @Test
    void shouldSerializedTodo() throws JsonProcessingException, JSONException {
        // Given
        final Todo givenTodo = new Todo(
                TodoId.from(new UUID(0, 0)),
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
                            "id": "00000000-0000-0000-0000-000000000000"
                          },
                          "description":"lorem ipsum",
                          "status":"IN_PROGRESS",
                          "important":false
                        }
                        """, serializedJson, JSONCompareMode.STRICT);
    }
}
