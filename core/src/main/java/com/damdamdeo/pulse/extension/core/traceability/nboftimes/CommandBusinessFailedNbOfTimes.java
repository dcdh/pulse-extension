package com.damdamdeo.pulse.extension.core.traceability.nboftimes;

public record CommandBusinessFailedNbOfTimes(int times) implements NbOfTimes {

    public static final CommandBusinessFailedNbOfTimes NEVER = new CommandBusinessFailedNbOfTimes(0);

    public static final CommandBusinessFailedNbOfTimes ONE = new CommandBusinessFailedNbOfTimes(1);

    public CommandBusinessFailedNbOfTimes {
        if (times < 0) {
            throw new IllegalArgumentException("times must be greater than or equal to 0");
        }
    }
}
