package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.AbstractRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.core.connecteduser.update.AbstractUpdateUserNameUseCase;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;

import java.util.List;

public sealed interface DomainUseCase<K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>>
        permits AbstractDomainUseCase, AbstractCreationalDomainUseCase, AbstractRegistrationDomainUseCase,
        AbstractUpdateUserNameUseCase, GuardDomainUseCase {

    A execute(C command) throws UseCaseException;

    List<Audience> audiences();
}
