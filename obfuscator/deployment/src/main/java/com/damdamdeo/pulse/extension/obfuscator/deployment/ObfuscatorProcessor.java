package com.damdamdeo.pulse.extension.obfuscator.deployment;

import com.damdamdeo.pulse.extension.compose.deployment.AdditionalVolumeBuildItem;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeProcessor;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.compose.runtime.datasource.PostgresUtils;
import com.damdamdeo.pulse.extension.obfuscator.runtime.*;
import com.damdamdeo.pulse.extension.obfuscator.runtime.annotation.DeObfuscatingParamConverterProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ObfuscatorProcessor {

    public static final Supplier<Boolean> hasQuarkusJdbcPostgresInClassPath = () -> QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.jdbc.postgresql.runtime.PostgreSQLAgroalConnectionConfigurer");

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans(final BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrorBuildItemBuildProducer) {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        if (hasQuarkusJdbcPostgresInClassPath.get()) {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(JdbcPostgresObfuscatorRepository.class).build());
        } else {
            validationErrorBuildItemBuildProducer.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new IllegalStateException("No Obfuscator found - please add io.quarkus:quarkus-jdbc-postgresql dependency")));
        }
        additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                .addBeanClasses(UUIDProvider.class, DefaultObfuscator.class, ObfuscatorObjectMapperCustomizer.class,
                        CachedObfuscator.class)
                .build());
        return additionalBeanBuildItems;
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClassesBuildItem() {
        return new AdditionalIndexedClassesBuildItem(DeObfuscatingParamConverterProvider.class.getName());
    }

    @BuildStep
    void generateCompose(final BuildProducer<ComposeServiceBuildItem> composeServiceBuildItemBuildProducer) {
        if (hasQuarkusJdbcPostgresInClassPath.get()) {
            composeServiceBuildItemBuildProducer.produce(ComposeProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM);
        }
    }

    @BuildStep
    void generateAdditionalVolumeBuildItem(final BuildProducer<AdditionalVolumeBuildItem> additionalVolumeBuildItemBuildProducer) {
        if (hasQuarkusJdbcPostgresInClassPath.get()) {
            final String schemaName = "pulse";
            additionalVolumeBuildItemBuildProducer.produce(new AdditionalVolumeBuildItem(
                    new ComposeServiceBuildItem.ServiceName(PostgresUtils.SERVICE_NAME),
                    new ComposeServiceBuildItem.Volume("./%s_obfuscator.sql".formatted(schemaName), "/docker-entrypoint-initdb.d/%s_obfuscator.sql".formatted(schemaName),
                            // language=sql
                            """
                                    CREATE SCHEMA IF NOT EXISTS %1$s;
                                    CREATE TABLE %1$s.obfuscator (
                                        original   TEXT NOT NULL,
                                        obfuscated UUID NOT NULL,
                                        CONSTRAINT obfuscator_original_unique UNIQUE (original)
                                    );
                                    CREATE INDEX idx_obfuscator_original ON %1$s.obfuscator (original);
                                    CREATE INDEX idx_obfuscator_obfuscated ON %1$s.obfuscator (obfuscated);
                                    """.formatted(schemaName).getBytes(StandardCharsets.UTF_8), "sql")));
        }
    }
}
