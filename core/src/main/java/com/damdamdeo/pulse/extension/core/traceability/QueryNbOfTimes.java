package com.damdamdeo.pulse.extension.core.traceability;

public record QueryNbOfTimes(int times) implements NbOfTimes {

    public static final QueryNbOfTimes ONE = new QueryNbOfTimes(1);

    public QueryNbOfTimes {
        if (times < 0) {
            throw new IllegalArgumentException("times must be greater than or equal to 0");
        }
    }
}
