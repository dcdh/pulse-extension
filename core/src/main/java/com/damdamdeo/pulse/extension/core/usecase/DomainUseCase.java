package com.damdamdeo.pulse.extension.core.usecase;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;

// TODO could have been called DomainUseCase
public interface DomainUseCase<K extends AggregateId, C extends Command<K>, A extends AggregateRoot<K>> extends UseCase<C, A> {

}
