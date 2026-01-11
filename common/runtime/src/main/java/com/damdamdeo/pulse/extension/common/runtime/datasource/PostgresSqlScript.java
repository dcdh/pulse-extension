package com.damdamdeo.pulse.extension.common.runtime.datasource;

import java.util.Objects;

public record PostgresSqlScript(String name, String content) {

    public PostgresSqlScript {
        Objects.requireNonNull(name);
        Objects.requireNonNull(content);
    }
}
