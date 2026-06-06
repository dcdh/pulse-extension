package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.Objects;

public record Title(Integer level, String content) implements Content {

    public Title {
        Objects.requireNonNull(level);
        Objects.requireNonNull(content);
    }

    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
}
