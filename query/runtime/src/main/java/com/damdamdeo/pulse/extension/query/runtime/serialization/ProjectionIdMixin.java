package com.damdamdeo.pulse.extension.query.runtime.serialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = ProjectionIdSerializer.class)
@JsonDeserialize(using = ProjectionIdDeserializer.class)
public abstract class ProjectionIdMixin {
}
