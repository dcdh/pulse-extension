package com.damdamdeo.pulse.extension.core;

import java.lang.annotation.*;

/**
 * Marks an optional or new field as nullable. In an event a new field is always nullable regarding the past!
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Nullable {
}
