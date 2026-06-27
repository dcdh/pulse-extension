package com.damdamdeo.pulse.extension.core.consumer;

import java.util.Objects;

public final class CdcTopicNaming {

    private final String topicName;

    private CdcTopicNaming(FromApplication fromApplication, Table table) {
        Objects.requireNonNull(fromApplication);
        Objects.requireNonNull(table);
        final String schema = SchemaName.from(fromApplication).name();
        this.topicName = "pulse.%s.%s".formatted(schema, table.name().toLowerCase());
    }

    public static CdcTopicNaming from(final FromApplication fromApplication, final Table table) {
        return new CdcTopicNaming(fromApplication, table);
    }

    public String name() {
        return topicName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CdcTopicNaming that = (CdcTopicNaming) o;
        return Objects.equals(topicName, that.topicName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(topicName);
    }

    @Override
    public String toString() {
        return "CdcTopicNaming{" +
                "topicName='" + topicName + '\'' +
                '}';
    }
}
