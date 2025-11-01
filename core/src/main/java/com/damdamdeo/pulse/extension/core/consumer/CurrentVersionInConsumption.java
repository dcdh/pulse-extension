package com.damdamdeo.pulse.extension.core.consumer;

import java.util.Objects;

public record CurrentVersionInConsumption(Integer version) {

    public CurrentVersionInConsumption {
        Objects.requireNonNull(version);
    }

    public boolean isFirstEvent() {
        return version == 0;
    }
}
