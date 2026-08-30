package com.damdamdeo.pulse.extension.core.usecase;

public interface UseCase<I, O> {

    O execute(I input) throws UseCaseException;
}
