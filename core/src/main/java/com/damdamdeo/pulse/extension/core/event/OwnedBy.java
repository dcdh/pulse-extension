package com.damdamdeo.pulse.extension.core.event;

import com.damdamdeo.pulse.extension.core.AggregateId;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

public record OwnedBy(String id) {

    private static final Pattern PATTERN = Pattern.compile("^[ a-zA-Z0-9_-]+$");

    public OwnedBy {
        Objects.requireNonNull(id);
        Validate.validState(PATTERN.matcher(id).matches(), "invalid id");
    }

    public static OwnedBy from(final AggregateId ownedBy) {
        Objects.requireNonNull(ownedBy);
        return new OwnedBy(ownedBy.id());
    }
}
