package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.BelongsTo;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.arc.Unremovable;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
@Unremovable
public final class PulseObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(final ObjectMapper objectMapper) {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        final SimpleModule module = new SimpleModule();
        module.addSerializer(SequenceNumber.class, new SequenceNumberSerializer());
        module.addDeserializer(SequenceNumber.class, new SequenceNumberDeserializer());
        module.addSerializer(OwnedBy.class, new OwnedBySerializer());
        module.addDeserializer(OwnedBy.class, new OwnedByDeserializer());
        module.addSerializer(BelongsTo.class, new BelongsToSerializer());
        module.addDeserializer(BelongsTo.class, new BelongsToDeserializer());
        objectMapper.registerModule(module);
    }
}
