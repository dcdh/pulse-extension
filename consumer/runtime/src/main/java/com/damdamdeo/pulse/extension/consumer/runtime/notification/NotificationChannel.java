package com.damdamdeo.pulse.extension.consumer.runtime.notification;

import com.damdamdeo.pulse.extension.consumer.runtime.Source;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NotificationChannel {

    /**
     * @return array of sources associated to consume for the notification channel
     */
    Source source();

    class Literal extends AnnotationLiteral<NotificationChannel> implements NotificationChannel {

        public static Literal of(final Source source) {
            return new Literal(source);
        }

        private final Source source;

        private Literal(final Source source) {
            this.source = Objects.requireNonNull(source);
        }

        @Override
        public Source source() {
            return source;
        }
    }
}
