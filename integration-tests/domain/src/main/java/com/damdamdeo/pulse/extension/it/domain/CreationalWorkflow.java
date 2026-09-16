package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.*;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.UserRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExceptionCode;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ApplicationScoped
public class CreationalWorkflow {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreationalWorkflow.class);

    private final UserRegistrationDomainUseCase userRegistrationDomainUseCase;
    private final CommandHandler<Todo, TodoId> todoCommandHandler;
    private final CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler;

    public CreationalWorkflow(final UserRegistrationDomainUseCase userRegistrationDomainUseCase,
                              final CommandHandler<Todo, TodoId> todoCommandHandler,
                              final CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler) {
        this.userRegistrationDomainUseCase = Objects.requireNonNull(userRegistrationDomainUseCase);
        this.todoCommandHandler = Objects.requireNonNull(todoCommandHandler);
        this.todoChecklistCommandHandler = Objects.requireNonNull(todoChecklistCommandHandler);
    }

    public Todo execute(final InitialiserCommand initialiserCommand) throws UseCaseException {
        Objects.requireNonNull(initialiserCommand);
        final User user = userRegistrationDomainUseCase.execute(new RegisterUser());
        LOGGER.info("User registered : {}", user);

        try {
            final Todo todoCreated = todoCommandHandler.handle(sequenceNumber -> new TodoId(user.id(), sequenceNumber),
                    new CreateTodo("lorem ipsum"), DuplicateTodoException::new);
            LOGGER.info("Todo created : {}", todoCreated);

            final TodoChecklist todoChecklistAdded = todoChecklistCommandHandler.handle(sequenceNumber -> new TodoChecklistId(todoCreated.id(), sequenceNumber),
                    new AddNewTodoItem(todoCreated.id(), "Make it works !"), DuplicateTodoChecklistException::new);
            LOGGER.info("TodoChecklist added : {}", todoChecklistAdded);
            return todoCreated;
        } catch (final CommandException exception) {
            throw new UseCaseException(exception, UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
