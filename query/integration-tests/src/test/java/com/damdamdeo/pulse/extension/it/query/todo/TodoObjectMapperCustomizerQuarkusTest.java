package com.damdamdeo.pulse.extension.it.query.todo;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.TodoChecklistProjection;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TodoObjectMapperCustomizerQuarkusTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void shouldRoundTripProjectionThroughJson() throws Exception {
        // Given
        final TodoProjection projection = new TodoProjection(ProjectionId.from(TodoId.USER_1_TODO_1),
                new Description("Description confidentielle"), Status.IN_PROGRESS, true,
                List.of(new TodoChecklistProjection(ProjectionId.from(TodoChecklistId.USER_1_TODO_1_1), "Checklist")),
                new Owner(UserId.USER_2));

        // When
        final String json = objectMapper.writeValueAsString(projection);
        final TodoProjection restored = objectMapper.readValue(json, TodoProjection.class);
        final JsonNode description = objectMapper.readTree(json).get("description");

        // Then
        assertAll(
                () -> assertEquals(projection, restored),
                () -> assertTrue(description.path("description_encrypted").isTextual()),
                () -> assertFalse(description.has("description")),
                () -> assertFalse(json.contains(projection.description().description())));
    }

}
