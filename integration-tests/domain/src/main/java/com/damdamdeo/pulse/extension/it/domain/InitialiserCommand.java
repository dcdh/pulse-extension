package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CreationalCommand;

public record InitialiserCommand() implements CreationalCommand<TodoId> {
}
