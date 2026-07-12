package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.Projection;

import java.util.List;

public record TodoProjection(
        TodoId todoId,
        String description,
        Status status,
        boolean important,
        List<TodoChecklistProjection> checklist) implements Projection {
}
