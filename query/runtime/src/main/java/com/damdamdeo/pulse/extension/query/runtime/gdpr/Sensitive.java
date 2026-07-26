package com.damdamdeo.pulse.extension.query.runtime.gdpr;

import com.fasterxml.jackson.annotation.JacksonAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface Sensitive {

    Mode value();

    enum Mode {
        ENCRYPT,
        ENCRYPT_AND_SEARCH_BY_HASH
    }
}
