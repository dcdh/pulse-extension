package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.List;
import java.util.Objects;

public record TableRow(List<String> content) {

    public TableRow {
        Objects.requireNonNull(content);
    }
}
