package com.damdamdeo.pulse.extension.publisher;

import org.apache.kafka.common.header.Headers;

public final class Record<K, V> {

    private final Headers headers;
    private final K key;
    private final V value;

    public Record(final Headers headers, final K key, final V value) {
        this.headers = headers;
        this.key = key;
        this.value = value;
    }

    public Headers getHeaders() {
        return headers;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
