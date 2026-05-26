package com.damdamdeo.pulse.extension.common.runtime;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class CommonConfigBuilder implements SmallRyeConfigBuilderCustomizer {

    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withMappingIgnore("pulse.**");
    }
}
