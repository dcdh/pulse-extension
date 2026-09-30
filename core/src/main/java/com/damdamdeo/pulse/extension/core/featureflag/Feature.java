package com.damdamdeo.pulse.extension.core.featureflag;

public interface Feature {

    String name();

    boolean isEnabled();
}
