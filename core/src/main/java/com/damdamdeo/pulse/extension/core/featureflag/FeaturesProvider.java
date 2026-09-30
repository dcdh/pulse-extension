package com.damdamdeo.pulse.extension.core.featureflag;

import java.util.List;

public interface FeaturesProvider {

    List<Feature> provideAll();
}
