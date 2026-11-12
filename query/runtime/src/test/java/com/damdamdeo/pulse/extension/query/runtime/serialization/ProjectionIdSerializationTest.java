package com.damdamdeo.pulse.extension.query.runtime.serialization;

import com.damdamdeo.pulse.extension.common.runtime.serialization.PulseObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectionIdSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        new PulseObjectMapperCustomizer().customize(objectMapper);
        new ProjectionIdObjectMapperCustomizer().customize(objectMapper);
    }

    @Test
    void shouldRoundTripRootProjectionIdUsingItsGenericType() throws Exception {
        final ProjectionId<UserId> projectionId = ProjectionId.from(UserId.USER_1);

        final String json = objectMapper.writeValueAsString(projectionId);
        final ProjectionId<UserId> restored = objectMapper.readValue(json, new TypeReference<>() {});

        assertAll(
                () -> assertEquals(projectionId, restored),
                () -> assertEquals(UserId.USER_1.id(), objectMapper.readTree(json).get("id").textValue()));
    }

    @Test
    void shouldPreserveProjectionIdDistinctFromLinkedAggregateId() throws Exception {
        final String id = UserId.USER_1.id() + "-projection";
        final ProjectionId<UserId> projectionId = ProjectionId.from(() -> id, UserId.USER_1);

        final String json = objectMapper.writeValueAsString(projectionId);
        final ProjectionId<UserId> restored = objectMapper.readValue(json, new TypeReference<>() {});

        assertAll(
                () -> assertEquals(id, restored.id().id()),
                () -> assertEquals(UserId.USER_1, restored.linkTo()));
    }

    @Test
    void shouldRejectProjectionIdWithoutConcreteGenericType() {
        assertThrows(JsonMappingException.class,
                () -> objectMapper.readValue("{}", ProjectionId.class));
    }
}
