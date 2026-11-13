package com.damdamdeo.pulse.extension.it.api.todo;

import com.damdamdeo.pulse.extension.core.Description;
import com.damdamdeo.pulse.extension.core.Owner;
import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.CreateTodoDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

@Path("/todos")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TodoEndpoint {

    private final CreateTodoDomainUseCase createTodoDomainUseCase;

    public TodoEndpoint(final CreateTodoDomainUseCase createTodoDomainUseCase) {
        this.createTodoDomainUseCase = Objects.requireNonNull(createTodoDomainUseCase);
    }

    @POST
    public Response createTodo(@FormParam("description") final Description description,
                               @FormParam("ownerId") final String ownerId) throws UseCaseException {
        final CreateTodo command = new CreateTodo(description, new Owner(UserId.from(ownerId)));
        final Todo todo = createTodoDomainUseCase.execute(command).aggregateRoot();
        return Response.status(Response.Status.CREATED).entity(new TodoDTO(todo)).build();
    }

    public record TodoDTO(String todoId, String description, Status status, boolean important, String ownerId) {

        public TodoDTO(final Todo todo) {
            this(todo.todoId().id(), todo.description().description(), todo.status(), todo.important(),
                    todo.owner().id().id());
        }
    }
}
