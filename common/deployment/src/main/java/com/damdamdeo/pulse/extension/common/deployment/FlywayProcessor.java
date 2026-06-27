package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

import java.util.Map;

public class FlywayProcessor {

    private static final String ORG_FLYWAYDB_GROUP_ID = "org.flywaydb";
    private static final String FLYWAY_DATABASE_POSTGRESQL_ARTIFACT_ID = "flyway-database-postgresql";

    private static final Map<String, String> FLYWAY_CONFIGURATIONS = Map.of(
            "quarkus.flyway.migrate-at-start", "true",
            "quarkus.flyway.baseline-on-migrate", "true");

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
                                   final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                   final BuildProducer<ContentBuildItem> contentBuildItemBuildProducer) {
        if (capabilities.isPresent(Capability.FLYWAY)) {
            FLYWAY_CONFIGURATIONS.forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer.produce(
                    new RunTimeConfigurationDefaultBuildItem(key, value)));

            contentBuildItemBuildProducer.produce(new ContentBuildItem(new Title(Title.Level.SECOND, "Flyway configuration")));
            contentBuildItemBuildProducer.produce(new ContentBuildItem(CodeBlock.fromProperties(FLYWAY_CONFIGURATIONS)));
        }
    }
}
