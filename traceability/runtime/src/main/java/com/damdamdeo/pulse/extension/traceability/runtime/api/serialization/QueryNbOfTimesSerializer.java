package com.damdamdeo.pulse.extension.traceability.runtime.api.serialization;

import com.damdamdeo.pulse.extension.core.traceability.nboftimes.QueryNbOfTimes;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Objects;

public class QueryNbOfTimesSerializer extends StdSerializer<QueryNbOfTimes> {

    public QueryNbOfTimesSerializer() {
        super(QueryNbOfTimes.class);
    }

    @Override
    public void serialize(final QueryNbOfTimes value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(gen);
        Objects.requireNonNull(provider);
        gen.writeNumber(value.times());
    }
}
