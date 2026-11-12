package com.damdamdeo.pulse.extension.it.api.todo;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.it.query.todo.TodoObjectMapperCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TodoDTOTest {

    // language=json
    private static final String JSON = """
            {
              "todoId": "U000001-T000001",
              "description": "Description publique",
              "status": "DONE",
              "important": true,
              "inProgress": false,
              "ownerId": "U000001"
            }
            """;

    private final ObjectMapper objectMapper = TodoObjectMapperCustomizer.internal(new ObjectMapper());

    @Test
    void shouldRoundTripDtoWithPlainDescriptionDespiteSensitiveMixin() throws Exception {
        // Given
        final TodoProjection projection = new TodoProjection(ProjectionId.from(TodoId.USER_1_TODO_1),
                new Description("Description publique"), Status.DONE, true, List.of(), Owner.USER_1);
        final TodoDTO dto = new TodoDTO(projection);

        // When
        final String json = objectMapper.writeValueAsString(dto);
        final TodoDTO restored = objectMapper.readValue(json, TodoDTO.class);

        // Then
        assertAll(
                () -> assertEquals(objectMapper.readTree(JSON), objectMapper.readTree(json)),
                () -> assertEquals(dto, restored),
                () -> assertEquals(projection.description(), dto.description()));
    }
}
