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

    private final List<Step<T>> steps = new ArrayList<>();

    public Checker(final Specification<T> firstSpec, final Function<T, ? extends RuntimeException> exceptionProvider) {
        next(firstSpec, exceptionProvider);
    }

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
}
