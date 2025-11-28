package com.damdamdeo.pulse.extension.writer.runtime;

import java.util.Map;

public abstract class EventClazzDiscovery {

    public Class<?> from(String eventClassName) throws ClassNotFoundException {
        final Map<String, String> mappings = mappings();
        if (!mappings.containsKey(eventClassName)) {
            throw new IllegalStateException("Should not be here !");
        } else {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            return cl.loadClass(mappings.get(eventClassName));
        }
    }

    protected abstract Map<String, String> mappings();
}
