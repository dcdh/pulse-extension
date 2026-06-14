package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.obfuscator.runtime.annotation.Obfuscate;

public record TodoProjection(@Obfuscate String todoId, String description, Status status, Boolean important) {
}
