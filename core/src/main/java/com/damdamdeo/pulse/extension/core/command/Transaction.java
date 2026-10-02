package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;

public interface Transaction {

    <A extends AggregateRoot<K>, K extends AggregateId> Handled<A, K> requiringNew(CommandCallable<Handled<A, K>> callable) throws CommandException;

    <A extends AggregateRoot<K>, K extends AggregateId> Handled<A, K> joiningExisting(CommandCallable<Handled<A, K>> callable) throws CommandException;
}
