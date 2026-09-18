package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.obfuscator.runtime.annotation.Obfuscate;

import java.util.Objects;

public record TodoProjection(@Obfuscate String id, @Obfuscate TodoId todoId, String description, Status status,
                             Boolean important) {

    public TodoProjection {
        Objects.requireNonNull(id);
        Objects.requireNonNull(todoId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(status);
        Objects.requireNonNull(important);
    }
}
