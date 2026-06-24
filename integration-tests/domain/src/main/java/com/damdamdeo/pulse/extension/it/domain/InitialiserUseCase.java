package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.AddNewTodoItem;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.UserRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class InitialiserUseCase implements UseCase<InitialiserCommand, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialiserUseCase.class);

    private final UserRegistrationDomainUseCase userRegistrationDomainUseCase;
    private final CommandHandler<Todo, TodoId> todoCommandHandler;
    private final CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler;

    public InitialiserUseCase(final UserRegistrationDomainUseCase userRegistrationDomainUseCase,
                              final CommandHandler<Todo, TodoId> todoCommandHandler,
                              final CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler) {
        this.userRegistrationDomainUseCase = Objects.requireNonNull(userRegistrationDomainUseCase);
        this.todoCommandHandler = Objects.requireNonNull(todoCommandHandler);
        this.todoChecklistCommandHandler = Objects.requireNonNull(todoChecklistCommandHandler);
    }

    @Override
    public Void execute(final InitialiserCommand initialiserCommand) throws BusinessException, TechnicalException {
        Objects.requireNonNull(initialiserCommand);
        final User user = userRegistrationDomainUseCase.execute(new RegisterUser());
        LOGGER.info("User registered : {}", user);

        final Todo todoCreated = todoCommandHandler.handle(sequenceNumber -> new TodoId(user.id(), sequenceNumber),
                new CreateTodo("lorem ipsum"), DuplicateTodoException::new);
        LOGGER.info("Todo created : {}", todoCreated);

        final TodoChecklist todoChecklistAdded = todoChecklistCommandHandler.handle(sequenceNumber -> new TodoChecklistId(todoCreated.id(), sequenceNumber),
                new AddNewTodoItem(todoCreated.id(), "Make it works !"), DuplicateTodoChecklistException::new);
        LOGGER.info("TodoChecklist added : {}", todoChecklistAdded);
        return null;
    }
}
