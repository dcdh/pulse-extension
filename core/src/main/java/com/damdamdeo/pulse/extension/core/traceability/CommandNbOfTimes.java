package com.damdamdeo.pulse.extension.core.traceability;

public record CommandNbOfTimes(int times) implements NbOfTimes {

    public static final CommandNbOfTimes ONE = new CommandNbOfTimes(1);

    public CommandNbOfTimes {
        if (times < 0) {
            throw new IllegalArgumentException("times must be greater than or equal to 0");
        }
    }
}
