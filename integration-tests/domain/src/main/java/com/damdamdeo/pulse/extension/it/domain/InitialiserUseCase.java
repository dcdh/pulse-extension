package com.damdamdeo.pulse.extension.it.domain;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.AddNewTodoItem;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProvider;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProviderException;
import com.damdamdeo.pulse.extension.core.usecase.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class InitialiserUseCase implements UseCase<InitialiserCommand, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialiserUseCase.class);

    private final ConnectedUserAggregateIdProvider connectedUserAggregateIdProvider;
    private final CommandHandler<User, UserId> userCommandHandler;
    private final CommandHandler<Todo, TodoId> todoCommandHandler;
    private final CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler;

    public InitialiserUseCase(final ConnectedUserAggregateIdProvider connectedUserAggregateIdProvider,
                              final CommandHandler<User, UserId> userCommandHandler,
                              final CommandHandler<Todo, TodoId> todoCommandHandler,
                              final CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler) {
        this.connectedUserAggregateIdProvider = Objects.requireNonNull(connectedUserAggregateIdProvider);
        this.userCommandHandler = Objects.requireNonNull(userCommandHandler);
        this.todoCommandHandler = Objects.requireNonNull(todoCommandHandler);
        this.todoChecklistCommandHandler = Objects.requireNonNull(todoChecklistCommandHandler);
    }

    @Override
    public Void execute(final InitialiserCommand initialiserCommand) throws BusinessException, TechnicalException {
        Objects.requireNonNull(initialiserCommand);
        try {
            final UserId provided = connectedUserAggregateIdProvider.provide(UserId.class, UserId::from, UserId::new);
            final User user = userCommandHandler.handle(provided, new RegisterUser(), DuplicateUserException::new);
            LOGGER.info("User registered : {}", user);

            final Todo todoCreated = todoCommandHandler.handle(sequenceNumber -> new TodoId(provided, sequenceNumber),
                    new CreateTodo("lorem ipsum"), DuplicateTodoException::new);
            LOGGER.info("Todo created : {}", todoCreated);

            final TodoChecklist todoChecklistAdded = todoChecklistCommandHandler.handle(sequenceNumber -> new TodoChecklistId(todoCreated.id(), sequenceNumber),
                    new AddNewTodoItem(todoCreated.id(), "Make it works !"), DuplicateTodoChecklistException::new);
            LOGGER.info("TodoChecklist added : {}", todoChecklistAdded);
            return null;
        } catch (final ConnectedUserAggregateIdProviderException exception) {
            throw new TechnicalException(exception);
        }
    }
}
