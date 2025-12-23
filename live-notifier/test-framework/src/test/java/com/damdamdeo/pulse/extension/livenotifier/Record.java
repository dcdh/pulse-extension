package com.damdamdeo.pulse.extension.livenotifier;

import org.apache.kafka.common.header.Headers;

public final class Record {

    private final Headers headers;
    private final String key;
    private final byte[] value;

    public Record(final Headers headers, final String key, final byte[] value) {
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

    public byte[] getValue() {
        return value;
    }
}
