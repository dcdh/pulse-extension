package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record FileIdentifier(String id) implements Identifiable {

    public FileIdentifier {
        Objects.requireNonNull(id);
        Validate.notBlank(id);
    }

    public static FileIdentifier from(final AggregateId id) {
        return new FileIdentifier(id.id());
    }

    public static FileIdentifier from(final String id) {
        return new FileIdentifier(id);
    }
}
