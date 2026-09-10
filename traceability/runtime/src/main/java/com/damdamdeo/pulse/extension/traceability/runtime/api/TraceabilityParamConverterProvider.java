package com.damdamdeo.pulse.extension.traceability.runtime.api;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Provider
public class TraceabilityParamConverterProvider implements ParamConverterProvider {

    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = Map.of(
            AnyAggregateId.class, AnyAggregateId::from,
            ExecutedByHashed.class, ExecutedByHashed::from
    );

    private static final Map<Class<?>, Function<Object, String>> SERIALIZERS = Map.of(
            AggregateId.class, value -> ((AggregateId) value).id()
    );

    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        final Function<String, ?> converter = CONVERTERS.get(rawType);
        final Function<Object, String> serializer = SERIALIZERS.get(rawType);
        if (converter == null) {
            return null;
        }
        return new ParamConverter<>() {

            @Override
            public T fromString(final String value) {
                Objects.requireNonNull(value);
                return rawType.cast(converter.apply(value));
            }

            @Override
            public String toString(final T value) {
                Objects.requireNonNull(value);
                return serializer.apply(value);
            }
        };
    }
}
