package com.damdamdeo.pulse.extension.it.infra;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.UserRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.it.domain.CreationalWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ApplicationProducers {

    @Produces
    @ApplicationScoped
    CreationalWorkflow creationalWorkflow(final UserRegistrationDomainUseCase userRegistrationDomainUseCase,
                                          final CommandHandler<TodoAggregateRoot, TodoId> todoCommandHandler,
                                          final CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler) {
        return new CreationalWorkflow(userRegistrationDomainUseCase, todoCommandHandler, todoChecklistCommandHandler);
    }
}
