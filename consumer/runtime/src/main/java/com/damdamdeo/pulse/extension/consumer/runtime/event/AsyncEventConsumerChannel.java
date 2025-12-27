package com.damdamdeo.pulse.extension.consumer.runtime.event;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AsyncEventConsumerChannel {

    /**
     * @return target consuming kafka channel likes "statistic"
     */
    String target();

    /**
     * @return array of sources associated with this event channel
     */
    @Nonbinding
    Source[] sources();

    /**
     * Represents a source entry with application and outbox table name.
     */
    @interface Source {

        String functionalDomain();

        String componentName();
    }

    class Literal extends AnnotationLiteral<AsyncEventConsumerChannel> implements AsyncEventConsumerChannel {

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

        @Override
        public Source[] sources() {
            return new Source[] {};
        }
    }
}
