package com.damdamdeo.pulse.extension.consumer.runtime;

/**
 * Represents a source entry with application and outbox table name.
 */
public @interface Source {

    public static final String APPLICATION_NAMING = "applicationNaming";

    String applicationNaming();
}
