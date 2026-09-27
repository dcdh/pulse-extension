package com.damdamdeo.pulse.extension.core.traceability.nboftimes;

public record CommandUnauthorizedNbOfTimes(int times) implements NbOfTimes {

    public static final CommandUnauthorizedNbOfTimes NEVER = new CommandUnauthorizedNbOfTimes(0);

    public static final CommandUnauthorizedNbOfTimes ONE = new CommandUnauthorizedNbOfTimes(1);

    public CommandUnauthorizedNbOfTimes {
        if (times < 0) {
            throw new IllegalArgumentException("times must be greater than or equal to 0");
        }
    }
}
