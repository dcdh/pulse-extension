package com.damdamdeo.pulse.extension.core.spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

// Chain of responsibility pattern.
// The first Specification returning false will throw the supplied exception.
public final class Checker<T> {

    private final List<Step<T>> steps = new ArrayList<>();

    private record Step<T>(Specification<T> spec, Function<T, ? extends RuntimeException> exceptionProvider) {

        private Step {
            Objects.requireNonNull(spec);
            Objects.requireNonNull(exceptionProvider);
        }
    }

    public Checker<T> next(final Specification<T> spec, final Function<T, ? extends RuntimeException> exceptionProvider) {
        Objects.requireNonNull(spec);
        Objects.requireNonNull(exceptionProvider);
        steps.add(new Step<>(spec, exceptionProvider));
        return this;
    }

    public void check(final T t) {
        for (final Step<T> step : steps) {
            if (!step.spec.isSatisfiedBy(t)) {
                throw step.exceptionProvider.apply(t);
            }
        }
    }

    public void check(final Supplier<T> supplier) {
        Objects.requireNonNull(supplier);
        this.check(supplier.get());
    }
}
