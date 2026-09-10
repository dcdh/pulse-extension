package com.damdamdeo.pulse.extension.core.traceability;

@FunctionalInterface
public interface FinderSupplier<T> {

    T get() throws TraceRepositoryException;
}
