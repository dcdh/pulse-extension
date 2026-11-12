package com.damdamdeo.pulse.extension.core;

public interface Todo {

    TodoId todoId();

    Description description();

    Status status();

    boolean important();

    boolean isInProgress();

    Owner owner();
}
