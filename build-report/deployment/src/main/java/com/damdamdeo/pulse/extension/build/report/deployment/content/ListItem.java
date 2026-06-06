package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.Objects;

public record ListItem(String content) {

    public ListItem {
        Objects.requireNonNull(content);
    }
}
