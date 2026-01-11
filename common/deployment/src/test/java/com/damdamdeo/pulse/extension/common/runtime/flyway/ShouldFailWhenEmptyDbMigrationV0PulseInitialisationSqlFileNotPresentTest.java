package com.damdamdeo.pulse.extension.common.runtime.flyway;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenEmptyDbMigrationV0PulseInitialisationSqlFileNotPresentTest {

    /**
     * Context of the test ...
     * Junit 5 do not flush target between each test.
     * Regarding this test I do not want to have a db/migration/V0__pulse_initialization.sql present from a previous test
     * The deleteTarget will only delete everything from db and including db too
     * <p>
     * The application using pulse must create an empty db/migration/V0__pulse_initialization.sql to make it discoverable by the classloader
     * in FlywayProcessor. This part is problematic
     * ClassPathUtils.consumeAsPaths(Thread.currentThread().getContextClassLoader(), location, path -> {
     * Set<String> applicationMigrations = null;
     * try {
     * applicationMigrations = FlywayProcessor.this.getApplicationMigrationsFromPath(finalLocation, path);
     * } catch (IOException e) {
     * LOGGER.warnv(e,
     * "Can't process files in path %s", path);
     * }
     * if (applicationMigrations != null) {
     * applicationMigrationResources.addAll(applicationMigrations);
     * }
     * });
     * because at least one migration file must be present.
     * An empty file must be provided. The definition will be done in the processor next in target part.
     */
    private static final Path TARGET = Paths.get("target/classes/db");

    static {
        deleteTarget();
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-flyway", Version.getVersion()),
                    Dependency.of("org.flywaydb", "flyway-database-postgresql", "11.12.0")
            ))
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Missing required Flyway migration: db/migration/V0__pulse_initialization.sql")
                    .hasNoSuppressedExceptions());

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }

    private static void deleteTarget() {
        if (Files.exists(TARGET)) {
            try (final Stream<Path> paths = Files.walk(TARGET)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
