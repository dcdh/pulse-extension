package com.damdamdeo.pulse.extension.consumer.runtime.event;

import com.damdamdeo.pulse.extension.consumer.runtime.Source;
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
    String purpose();

    /**
     * @return array of sources associated with this event channel
     */
    @Nonbinding
    Source[] sources();

    class Literal extends AnnotationLiteral<AsyncEventConsumerChannel> implements AsyncEventConsumerChannel {

        public static Literal of(final String purpose) {
            return new Literal(purpose);
        }

        private final String purpose;

        private Literal(final String purpose) {
            this.purpose = Objects.requireNonNull(purpose);
        }

        @Override
        public String purpose() {
            return purpose;
        }

        @Override
        public Source[] sources() {
            return new Source[]{};
        }
    }
}
