package com.damdamdeo.pulse.extension.common.runtime.featureflag;

import com.damdamdeo.pulse.extension.core.featureflag.Feature;
import com.damdamdeo.pulse.extension.core.featureflag.FeaturesProvider;
import io.quarkus.arc.All;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
@Unremovable
public class ArcFeaturesProvider implements FeaturesProvider {

    @Inject
    @All
    List<Feature> features;

    @Override
    public List<Feature> provideAll() {
        return features;
    }
}
