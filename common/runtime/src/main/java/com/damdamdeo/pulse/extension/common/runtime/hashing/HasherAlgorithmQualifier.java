package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Algorithm;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface HasherAlgorithmQualifier {

    Algorithm value();

    class Literal extends AnnotationLiteral<HasherAlgorithmQualifier> implements HasherAlgorithmQualifier {

        private final Algorithm value;

        public Literal(final Algorithm value) {
            this.value = Objects.requireNonNull(value);
        }

        public static Literal of(final Algorithm value) {
            return new Literal(value);
        }

        @Override
        public Algorithm value() {
            return value;
        }
    }
}
