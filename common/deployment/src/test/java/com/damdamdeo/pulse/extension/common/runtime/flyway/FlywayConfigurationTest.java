package com.damdamdeo.pulse.extension.common.runtime.flyway;

import com.damdamdeo.pulse.extension.common.deployment.FlywayProcessor;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class FlywayConfigurationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(EmptyAsset.INSTANCE, FlywayProcessor.FLYWAY_V0_LOCATION))
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-flyway", Version.getVersion()),
                    Dependency.of("org.flywaydb", "flyway-database-postgresql", "11.12.0")
            ));


    @Test
    void shouldGenerateFlywayConfiguration() {
        assertAll(
                () -> assertThat(ConfigProvider.getConfig().getValue("quarkus.flyway.migrate-at-start", Boolean.class))
                        .isTrue(),
                () -> assertThat(ConfigProvider.getConfig().getValue("quarkus.flyway.baseline-on-migrate", Boolean.class))
                        .isTrue()
        );
    }
}
