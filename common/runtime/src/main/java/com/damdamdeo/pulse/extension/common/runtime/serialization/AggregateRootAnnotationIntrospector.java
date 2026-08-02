package com.damdamdeo.pulse.extension.common.runtime.serialization;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public final class AggregateRootAnnotationIntrospector extends JacksonAnnotationIntrospector {

    public static final String BELONGS_TO = "belongsTo";
    public static final String OWNED_BY = "ownedBy";

    @Override
    public PropertyName findNameForSerialization(final Annotated annotated) {
        final PropertyName propertyName = super.findNameForSerialization(annotated);
        if (propertyName != null) {
            return propertyName;
        }
        if (annotated instanceof AnnotatedMethod method
                && AggregateRoot.class.isAssignableFrom(method.getDeclaringClass())
                && method.getParameterCount() == 0) {
            return switch (method.getName()) {
                case BELONGS_TO -> PropertyName.construct(BELONGS_TO);
                case OWNED_BY -> PropertyName.construct(OWNED_BY);
                default -> null;
            };
        }
        return null;
    }
}
