package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.Objects;

public record Admonition(AdmonitionType admonitionType, String content) implements Content {

    public Admonition {
        Objects.requireNonNull(admonitionType);
        Objects.requireNonNull(content);
    }

    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
}
