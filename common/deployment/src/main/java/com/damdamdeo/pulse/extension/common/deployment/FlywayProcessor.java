package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.util.ClassPathUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlywayProcessor {

    private static final String ORG_FLYWAYDB_GROUP_ID = "org.flywaydb";
    private static final String FLYWAY_DATABASE_POSTGRESQL_ARTIFACT_ID = "flyway-database-postgresql";

    @BuildStep
    void validateFlywayDependency(final Capabilities capabilities,
                                  final CurateOutcomeBuildItem curateOutcomeBuildItem,
                                  final BuildProducer<ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (capabilities.isPresent(Capability.FLYWAY) && curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                .noneMatch(dep -> ORG_FLYWAYDB_GROUP_ID.equals(dep.getGroupId())
                        && FLYWAY_DATABASE_POSTGRESQL_ARTIFACT_ID.equals(dep.getArtifactId()))) {
            validationErrorBuildItemProducer.produce(new ValidationErrorBuildItem(
                    new IllegalStateException("Missing maven dependency %s:%s".formatted(ORG_FLYWAYDB_GROUP_ID, FLYWAY_DATABASE_POSTGRESQL_ARTIFACT_ID))));
        }
    }

    @BuildStep
    void defineFlywayConfiguration(final Capabilities capabilities,
                                   final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemProducer) {
        if (capabilities.isPresent(Capability.FLYWAY)) {
            runTimeConfigurationDefaultBuildItemProducer.produce(
                    new RunTimeConfigurationDefaultBuildItem("quarkus.flyway.migrate-at-start", "true"));
            runTimeConfigurationDefaultBuildItemProducer.produce(
                    new RunTimeConfigurationDefaultBuildItem("quarkus.flyway.baseline-on-migrate", "true"));
        }
    }

    public static final String FLYWAY_V0_LOCATION = "db/migration/V0__pulse_initialization.sql";

    @BuildStep
    void validateVoPresence(final Capabilities capabilities,
                            final BuildProducer<ValidationErrorBuildItem> validationErrorBuildItemProducer) throws IOException {
        if (capabilities.isPresent(Capability.FLYWAY)) {
            final AtomicBoolean found = new AtomicBoolean(false);
            ClassPathUtils.consumeAsStreams(
                    Thread.currentThread().getContextClassLoader(),
                    FLYWAY_V0_LOCATION,
                    inputStream -> {
                        found.set(true);
                    }
            );
            if (!found.get()) {
                validationErrorBuildItemProducer.produce(
                        new ValidationErrorBuildItem(
                                new IllegalStateException(
                                        "Missing required Flyway migration: " + FLYWAY_V0_LOCATION
                                )
                        )
                );
            }
        }
    }
}
