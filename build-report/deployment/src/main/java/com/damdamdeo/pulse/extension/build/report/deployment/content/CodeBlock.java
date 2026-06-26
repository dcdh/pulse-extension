package com.damdamdeo.pulse.extension.build.report.deployment.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record CodeBlock(String source, String content) implements Content {

    public CodeBlock {
        Objects.requireNonNull(source);
        Objects.requireNonNull(content);
    }

    public static CodeBlock fromProperties(final Map<String, String> properties) {
        Objects.requireNonNull(properties);
        return new CodeBlock("properties",
                properties.entrySet().stream().map(property -> property.getKey() + "=" + property.getValue())
                        .collect(Collectors.joining("\n")));
    }

    public static CodeBlock fromYaml(final String content) {
        Objects.requireNonNull(content);
        return new CodeBlock("yaml", content);
    }

    public static CodeBlock fromJson(final Object objectToJsonStringify, final ObjectMapper objectMapper) throws JsonProcessingException {
        Objects.requireNonNull(objectToJsonStringify);
        Objects.requireNonNull(objectMapper);
        final String prettyPrintContent = objectMapper
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(objectToJsonStringify);
        return new CodeBlock("json", prettyPrintContent);
    }

    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
}
