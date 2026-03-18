package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.spec.Checker;
import com.damdamdeo.pulse.extension.core.spec.Step;
import com.damdamdeo.pulse.extension.core.spec.TodoIsInProgressSpec;

public class TodoChecker {

    public static final Checker<Todo> TODO_IN_PROGRESS = Checker.<Todo>builder()
            .step(Step.<Todo>builder()
                    .spec(new TodoIsInProgressSpec())
                    .exceptionProvider((todo) -> new IllegalStateException("la todo %s doit être in progress".formatted(todo.id().id())))
                    .build())
            .build();
}
