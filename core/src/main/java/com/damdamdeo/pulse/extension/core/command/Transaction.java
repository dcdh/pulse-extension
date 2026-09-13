package com.damdamdeo.pulse.extension.core.command;

import com.damdamdeo.pulse.extension.core.AggregateRoot;

public interface Transaction {

    <A extends AggregateRoot<?>> A requiringNew(CommandCallable<A> callable) throws CommandException;

    <A extends AggregateRoot<?>> A joiningExisting(CommandCallable<A> callable) throws CommandException;
}
