package com.damdamdeo.pulse.extension.publisher;

import org.apache.kafka.common.header.Headers;

public final class Record {

    private final Headers headers;
    private final JsonNodeEventKey key;
    private final JsonNodeEventValue value;

    public Record(final Headers headers, final JsonNodeEventKey key, final JsonNodeEventValue value) {
        this.headers = headers;
        this.key = key;
        this.value = value;
    }

    public Headers getHeaders() {
        return headers;
    }

    public JsonNodeEventKey getKey() {
        return key;
    }

    public JsonNodeEventValue getValue() {
        return value;
    }
}
