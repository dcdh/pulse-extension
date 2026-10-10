package com.damdamdeo.pulse.extension.core.traceability.nboftimes;

public record TraceabilityNbOfTimes(int times) implements NbOfTimes {

    public static final TraceabilityNbOfTimes NEVER = new TraceabilityNbOfTimes(0);

    public static final TraceabilityNbOfTimes ONE = new TraceabilityNbOfTimes(1);

    public TraceabilityNbOfTimes {
        if (times < 0) {
            throw new IllegalArgumentException("times must be greater than or equal to 0");
        }
    }
}
