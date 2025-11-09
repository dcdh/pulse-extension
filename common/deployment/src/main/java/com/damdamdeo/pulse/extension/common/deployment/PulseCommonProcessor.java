package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.datasource.InitScriptUsageChecker;
import com.damdamdeo.pulse.extension.common.runtime.datasource.PostgresqlSchemaInitializer;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseGenerator;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseProvider;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.common.runtime.serialization.AllFieldsVisibilityObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.common.runtime.vault.VaultPassphraseRepository;
import com.damdamdeo.pulse.extension.core.consumer.ApplicationNaming;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

import java.util.List;

public class PulseCommonProcessor {

    private static final String FEATURE = "pulse-common-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(AllFieldsVisibilityObjectMapperCustomizer.class)
                .addBeanClasses(VaultPassphraseRepository.class, DefaultPassphraseGenerator.class,
                        DefaultPassphraseProvider.class, OpenPGPDecryptionService.class,
                        OpenPGPEncryptionService.class,
                        InitScriptUsageChecker.class, PostgresqlSchemaInitializer.class)
                .build();
    }

    @BuildStep
    void validateApplicationNaming(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                   final BuildProducer<ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (!ApplicationNaming.FULL_PATTERN.matcher(applicationInfoBuildItem.getName()).matches()) {
            validationErrorBuildItemProducer.produce(new ValidationErrorBuildItem(
                    new IllegalArgumentException(
                            "Invalid application name '%s' - it should match '%s'".formatted(applicationInfoBuildItem.getName(), ApplicationNaming.FULL_PATTERN.pattern()))));
        }
    }

    @BuildStep
    void mapToQuarkusValidationErrorBuildItem(final List<ValidationErrorBuildItem> validationErrorBuildItems,
                                              final BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (!validationErrorBuildItems.isEmpty()) {
            validationErrorBuildItemProducer.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    validationErrorBuildItems.stream().map(ValidationErrorBuildItem::getCause).toList()));
        }
    }

    @BuildStep
    RunTimeConfigurationDefaultBuildItem definePostgresCurrentSchema(final ApplicationInfoBuildItem applicationInfoBuildItem) {
        return new RunTimeConfigurationDefaultBuildItem("quarkus.datasource.jdbc.additional-jdbc-properties.currentSchema",
                applicationInfoBuildItem.getName().toLowerCase());
    }

    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> defaultConfigurations() {
        return List.of(
                new RunTimeConfigurationDefaultBuildItem("quarkus.datasource.jdbc.max-size", "100"));
    }
}
