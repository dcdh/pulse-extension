package com.damdamdeo.pulse.extension.common.runtime.flyway;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenFlywayDatabasePostgresqlIsMissingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
//                    Dependency.of("io.quarkus", "quarkus-jdbc-postgresql", Version.getVersion()),
// The optional dependency org.flywaydb:flyway-database-postgresql will be taken into the build
                    Dependency.of("io.quarkus", "quarkus-flyway", Version.getVersion())
            ))
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Missing maven dependency org.flywaydb:flyway-database-postgresql")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }
}
