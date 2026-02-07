package com.damdamdeo.pulse.extension.core.spec;

import com.damdamdeo.pulse.extension.core.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

// Chain of responsibility pattern.
// The first Specification returning false will throw the supplied exception.
public final class Checker<T> {

    private final List<Step<T>> steps;

    private Checker() {
        throw new UnsupportedOperationException("Use builder");
    }

    private Checker(final List<Step<T>> steps) {
        Objects.requireNonNull(steps);
        this.steps = steps;
    }

    private record Step<T>(Specification<T> spec, Function<T, ? extends RuntimeException> exceptionProvider) {

        private Step {
            Objects.requireNonNull(spec);
            Objects.requireNonNull(exceptionProvider);
        }
    }

    public void check(final T t, final ExecutionContext executionContext) {
        Objects.requireNonNull(executionContext);
        for (final Step<T> step : steps) {
            if (!step.spec.isSatisfiedBy(t, executionContext)) {
                throw step.exceptionProvider.apply(t);
            }
        }
    }

    public void check(final Supplier<T> supplier, final ExecutionContext executionContext) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(executionContext);
        this.check(supplier.get(), executionContext);
    }

    public boolean isSatisfiedBy(final T t, final ExecutionContext executionContext) {
        Objects.requireNonNull(executionContext);
        for (final Step<T> step : steps) {
            if (!step.spec.isSatisfiedBy(t, executionContext)) {
                return false;
            }
        }
        return true;
    }

    public boolean isSatisfiedBy(final Supplier<T> supplier, final ExecutionContext executionContext) {
        Objects.requireNonNull(supplier);
        return this.isSatisfiedBy(supplier.get(), executionContext);
    }

    // ----------------------------
    // Builder
    // ----------------------------

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {

        private final List<Step<T>> steps = new ArrayList<>();

        private Builder() {
        }

        public Builder<T> step(final Specification<T> spec,
                               final Function<T, ? extends RuntimeException> exceptionProvider) {
            Objects.requireNonNull(spec);
            Objects.requireNonNull(exceptionProvider);
            steps.add(new Step<>(spec, exceptionProvider));
            return this;
        }

        public Checker<T> build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("Checker must contain at least one step.");
            }
            return new Checker<>(steps);
        }
    }
}
