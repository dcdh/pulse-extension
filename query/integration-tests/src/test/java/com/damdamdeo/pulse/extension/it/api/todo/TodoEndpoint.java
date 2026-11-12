package com.damdamdeo.pulse.extension.it.api.todo;

import com.damdamdeo.pulse.extension.core.pagination.Pagination;
import com.damdamdeo.pulse.extension.core.query.ListDoneTodos;
import com.damdamdeo.pulse.extension.core.query.ListDoneTodosQueryUseCase;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Objects;

@Path("/todos")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TodoEndpoint {

    private final ListDoneTodosQueryUseCase listDoneTodosQueryUseCase;

    public TodoEndpoint(final ListDoneTodosQueryUseCase listDoneTodosQueryUseCase) {
        this.listDoneTodosQueryUseCase = Objects.requireNonNull(listDoneTodosQueryUseCase);
    }

    @GET
    @Path("/done")
    public List<TodoDTO> listDoneTodos() throws QueryException {
        return listDoneTodosQueryUseCase.execute(new ListDoneTodos(), Pagination.all()).projections().stream()
                .map(TodoDTO::new)
                .toList();
    }
}
