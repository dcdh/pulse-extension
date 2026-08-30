package com.damdamdeo.pulse.extension.core.command;

@FunctionalInterface
public interface CommandCallable<T> {

    T call() throws CommandException;
}
