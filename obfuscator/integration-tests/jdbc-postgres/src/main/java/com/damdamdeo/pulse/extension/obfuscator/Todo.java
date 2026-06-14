package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;

public record Todo(TodoId todoId, String description, Status status, Boolean important) {

}
