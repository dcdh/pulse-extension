package com.damdamdeo.pulse.extension.common.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

public final class ValidationErrorBuildItem extends MultiBuildItem {

    private final Throwable cause;

    public ValidationErrorBuildItem(final Throwable cause) {
        this.cause = cause;
    }

    public Throwable getCause() {
        return cause;
    }
}
