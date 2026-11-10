package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.spec.Checker;
import com.damdamdeo.pulse.extension.core.spec.Step;
import com.damdamdeo.pulse.extension.core.spec.TodoIsInProgressSpec;

public class TodoChecker {

    public static final Checker<TodoAggregateRoot> TODO_IN_PROGRESS = Checker.<TodoAggregateRoot>builder()
            .step(Step.<TodoAggregateRoot>builder()
                    .spec(new TodoIsInProgressSpec())
                    .exceptionProvider((todo) -> new IllegalStateException("la todo %s doit être in progress".formatted(todo.id().id())))
                    .build())
            .build();
}
