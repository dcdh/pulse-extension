package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

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
}
