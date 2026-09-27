package com.damdamdeo.pulse.extension.core.traceability.nboftimes;

public record QueryUnauthorizedNbOfTimes(int times) implements NbOfTimes {

    public static final QueryUnauthorizedNbOfTimes NEVER = new QueryUnauthorizedNbOfTimes(0);

    public static final QueryUnauthorizedNbOfTimes ONE = new QueryUnauthorizedNbOfTimes(1);

    public QueryUnauthorizedNbOfTimes {
        if (times < 0) {
            throw new IllegalArgumentException("times must be greater than or equal to 0");
        }
    }
}
