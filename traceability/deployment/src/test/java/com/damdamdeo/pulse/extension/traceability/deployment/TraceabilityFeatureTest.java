package com.damdamdeo.pulse.extension.traceability.deployment;

import com.damdamdeo.pulse.extension.common.runtime.featureflag.ArcFeaturesProvider;
import com.damdamdeo.pulse.extension.core.featureflag.Feature;
import com.damdamdeo.pulse.extension.core.traceability.TraceabilityFeature;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TraceabilityFeatureTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    @Inject
    ArcFeaturesProvider arcFeaturesProvider;

    @Inject
    Instance<TraceabilityFeature> traceabilityFeature;

    @Test
    void shouldTraceabilityFeatureBeResolvable() {
        assertThat(traceabilityFeature.isResolvable()).isTrue();
    }

    @Test
    void shouldProvideTraceabilityFeatures() {
        // Given

        // When
        final List<String> features = arcFeaturesProvider.provideAll().stream().map(Feature::name).toList();

        // Then
        assertThat(features).contains("TRACEABILITY");
    }
}
