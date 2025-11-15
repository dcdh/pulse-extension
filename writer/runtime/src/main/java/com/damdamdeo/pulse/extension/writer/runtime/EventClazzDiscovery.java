package com.damdamdeo.pulse.extension.writer.runtime;

import java.util.Map;

public abstract class EventClazzDiscovery {

    public Class<?> from(String eventClassName) throws ClassNotFoundException {
        final Map<String, String> mappings = mappings();
        if (!mappings.containsKey(eventClassName)) {
            throw new IllegalStateException("Should not be here !");
        } else {
            return Class.forName(mappings.get(eventClassName));
        }
    }

    protected abstract Map<String, String> mappings();
}
