package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.Objects;

public record Paragraph(String content) implements Content {

    public Paragraph {
        Objects.requireNonNull(content);
    }

    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
}
