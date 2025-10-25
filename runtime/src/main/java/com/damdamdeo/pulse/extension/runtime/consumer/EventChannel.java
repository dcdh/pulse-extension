package com.damdamdeo.pulse.extension.runtime.consumer;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface EventChannel {

    /**
     * @return target consuming kafka channel likes "statistic"
     */
    String target();

    class Literal extends AnnotationLiteral<EventChannel> implements EventChannel {

        public static Literal of(final String target) {
            return new Literal(target);
        }

        private final String target;

        private Literal(final String target) {
            this.target = Objects.requireNonNull(target);
        }

        @Override
        public String target() {
            return target;
        }
    }
}
