package com.damdamdeo.pulse.extension.query.runtime.serialization;

import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProjectionIdObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(final ObjectMapper objectMapper) {
        objectMapper.addMixIn(ProjectionId.class, ProjectionIdMixin.class);
    }
}
