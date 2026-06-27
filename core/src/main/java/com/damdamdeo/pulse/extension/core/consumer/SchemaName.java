package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.ApplicationNaming;

import java.util.Objects;
import java.util.stream.Collectors;

public final class SchemaName {

    private final String name;

    private SchemaName(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    public static SchemaName from(final ApplicationNaming applicationNaming) {
        Objects.requireNonNull(applicationNaming);
        return new SchemaName(applicationNaming.split().stream().map(String::toLowerCase)
                .collect(Collectors.joining("_")));
    }

    public static SchemaName from(final FromApplication fromApplication) {
        Objects.requireNonNull(fromApplication);
        return from(fromApplication.applicationNaming());
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SchemaName that = (SchemaName) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "SchemaName{" +
                "name='" + name + '\'' +
                '}';
    }
}
