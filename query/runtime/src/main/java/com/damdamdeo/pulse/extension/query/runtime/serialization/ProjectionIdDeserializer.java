package com.damdamdeo.pulse.extension.query.runtime.serialization;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class ProjectionIdDeserializer extends StdDeserializer<ProjectionId<?>> implements ContextualDeserializer {

    private final JavaType aggregateType;

    public ProjectionIdDeserializer() {
        this(null);
    }

    private ProjectionIdDeserializer(final JavaType aggregateType) {
        super(ProjectionId.class);
        this.aggregateType = aggregateType;
    }

    @Override
    public JsonDeserializer<?> createContextual(final DeserializationContext context,
                                                final BeanProperty property) throws JsonMappingException {
        final JavaType projectionType = context.getContextualType();
        final JavaType linkToType = projectionType == null ? null : projectionType.containedType(0);
        if (linkToType == null || linkToType.isAbstract() || !linkToType.isTypeOrSubTypeOf(AggregateId.class)) {
            return context.reportBadDefinition(projectionType,
                    "ProjectionId requires a concrete AggregateId type parameter");
        }
        return new ProjectionIdDeserializer(linkToType);
    }

    @Override
    public ProjectionId<?> deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
        final JsonNode json = parser.readValueAsTree();
        if (!json.path("id").isTextual() || !json.hasNonNull("linkTo")) {
            return context.reportInputMismatch(ProjectionId.class, "ProjectionId requires id and linkTo properties");
        }
        final String id = json.get("id").textValue();
        final AggregateId linkTo;
        try (final JsonParser linkToParser = json.get("linkTo").traverse(parser.getCodec())) {
            linkToParser.nextToken();
            linkTo = context.readValue(linkToParser, aggregateType);
        }
        if (!id.startsWith(linkTo.id())) {
            return context.reportInputMismatch(ProjectionId.class, "ProjectionId id must start with linkTo id");
        }
        return id.equals(linkTo.id()) ? ProjectionId.from(linkTo) : ProjectionId.from(() -> id, linkTo);
    }
}
