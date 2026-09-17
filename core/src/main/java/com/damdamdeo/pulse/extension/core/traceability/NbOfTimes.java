package com.damdamdeo.pulse.extension.core.traceability;

public record NbOfTimes(int times) {

    public static final NbOfTimes ONE = new NbOfTimes(1);

    public NbOfTimes {
        if (times < 0) {
            throw new IllegalArgumentException("times must be greater than or equal to 0");
        }
    }
}
