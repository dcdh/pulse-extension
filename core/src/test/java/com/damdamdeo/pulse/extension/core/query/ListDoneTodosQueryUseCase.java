package com.damdamdeo.pulse.extension.core.query;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.permission.ExecutedBySpecificJob;
import com.damdamdeo.pulse.extension.core.query.permission.Permission;

import java.util.List;
import java.util.Objects;

public class ListDoneTodosQueryUseCase implements QueryUseCase<TodoId, ListDoneTodos, TodoProjection, MultiplePageableResult<TodoId, TodoProjection>> {

    private final TodoRepository todoRepository;

    public ListDoneTodosQueryUseCase(final TodoRepository todoRepository) {
        this.todoRepository = Objects.requireNonNull(todoRepository);
    }

    @Override
    public MultiplePageableResult<TodoId, TodoProjection> execute(final ListDoneTodos input, final Pagination pagination) throws QueryException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(pagination);
        final List<TodoProjection> doneTodos = todoRepository.findAll().stream()
                .filter(todo -> todo.status().equals(Status.DONE)).toList();
        return new MultiplePageableResult<>(new Page<>(doneTodos, pagination, 1L));
    }

    @Override
    public List<Permission<TodoId>> permissions() {
        return List.of(
                new ExecutedBySpecificJob<>(new JobName("ArchiveTodoSimpleScheduledJob")),
                new ExecutedBySpecificJob<>(new JobName("archiveTodosJob")));
    }
}
