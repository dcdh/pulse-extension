package com.damdamdeo.pulse.extension.core.spec;

import java.util.Objects;
import java.util.function.Function;

public record Step<T>(Specification<T> spec, Function<T, ? extends RuntimeException> exceptionProvider) {

    public Step {
        Objects.requireNonNull(spec);
        Objects.requireNonNull(exceptionProvider);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private Specification<T> spec;
        private Function<T, ? extends RuntimeException> exceptionProvider;

        public Builder<T> spec(final Specification<T> spec) {
            this.spec = spec;
            return this;
        }

        public Builder<T> exceptionProvider(final Function<T, ? extends RuntimeException> exceptionProvider) {
            this.exceptionProvider = exceptionProvider;
            return this;
        }

        public Step<T> build() {
            Objects.requireNonNull(spec);
            Objects.requireNonNull(exceptionProvider);
            return new Step<>(spec, exceptionProvider);
        }
    }
}
