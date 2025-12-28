package com.damdamdeo.pulse.extension.consumer.runtime.aggregateroot;

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
public @interface AsyncAggregateRootConsumerChannel {

    /**
     * @return target consuming kafka channel likes "statistic"
     */
    String target();

    /**
     * @return array of sources associated with this event channel
     */
    @Nonbinding
    Source[] sources();

    class Literal extends AnnotationLiteral<AsyncAggregateRootConsumerChannel> implements AsyncAggregateRootConsumerChannel {

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
            return new Source[]{};
        }
    }
}
