package com.damdamdeo.pulse.extension.it.infra;

import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.OwnerQuery;
import com.damdamdeo.pulse.extension.core.TodoId;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultOwnerQuery implements OwnerQuery {

    @Override
    public Owner getByTodoId(TodoId todoId) {
        throw new UnsupportedOperationException();
    }
}
