package com.damdamdeo.pulse.extension.livenotifier;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.header.Headers;

public final class Record {

    private final Headers headers;
    private final String key;
    private final JsonNode value;

    public Record(final Headers headers, final String key, final JsonNode value) {
        this.headers = headers;
        this.key = key;
        this.value = value;
    }

    public Headers getHeaders() {
        return headers;
    }

    public String getKey() {
        return key;
    }

    public JsonNode getValue() {
        return value;
    }
}
