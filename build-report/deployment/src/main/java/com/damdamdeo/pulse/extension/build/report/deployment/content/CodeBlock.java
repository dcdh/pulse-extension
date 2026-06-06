package com.damdamdeo.pulse.extension.build.report.deployment.content;

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
        return new CodeBlock("yaml", content);
    }

    public static CodeBlock fromJson(final String content) {
        return new CodeBlock("json", content);
    }

    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
}
