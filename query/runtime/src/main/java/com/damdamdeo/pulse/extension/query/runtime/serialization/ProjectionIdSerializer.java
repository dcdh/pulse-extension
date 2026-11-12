package com.damdamdeo.pulse.extension.query.runtime.serialization;

import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ProjectionIdSerializer extends StdSerializer<ProjectionId<?>> {

    public ProjectionIdSerializer() {
        super(ProjectionId.class, false);
    }

    @Override
    public void serialize(final ProjectionId<?> value, final JsonGenerator generator,
                          final SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("id", value.id().id());
        generator.writeFieldName("linkTo");
        provider.defaultSerializeValue(value.linkTo(), generator);
        generator.writeEndObject();
    }
}
