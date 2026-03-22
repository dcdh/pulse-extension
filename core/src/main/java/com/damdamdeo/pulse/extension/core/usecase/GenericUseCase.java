package com.damdamdeo.pulse.extension.core.usecase;

public interface GenericUseCase<I, O> {

    O execute(I input);
}
