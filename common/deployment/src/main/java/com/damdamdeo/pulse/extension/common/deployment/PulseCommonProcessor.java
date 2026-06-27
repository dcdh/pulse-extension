package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.ContentBuildItem;
import com.damdamdeo.pulse.extension.build.report.deployment.content.CodeBlock;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseGenerator;
import com.damdamdeo.pulse.extension.common.runtime.encryption.DefaultPassphraseProvider;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService;
import com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPEncryptionService;
import com.damdamdeo.pulse.extension.common.runtime.serialization.BusinessObjectMapperProducer;
import com.damdamdeo.pulse.extension.common.runtime.serialization.PulseObjectMapperCustomizer;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeProcessor;
import com.damdamdeo.pulse.extension.compose.deployment.ComposeServiceBuildItem;
import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.SchemaName;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.security.deployment.BouncyCastleProviderBuildItem;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PulseCommonProcessor {

    private static Logger LOG = Logger.getLogger(PulseCommonProcessor.class);

    private static final String FEATURE = "pulse-common-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        PulseObjectMapperCustomizer.class,
                        BusinessObjectMapperProducer.class,
                        DefaultPassphraseGenerator.class,
                        DefaultPassphraseProvider.class,
                        OpenPGPDecryptionService.class,
                        OpenPGPEncryptionService.class)
                .build());
        return additionalBeanBuildItems;
    }

    @BuildStep
    void validateApplicationNaming(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                   final BuildProducer<ValidationErrorBuildItem> validationErrorBuildItemProducer) {
        if (!ApplicationNaming.UPPER_CAMEL_CASE_PATTERN.matcher(applicationInfoBuildItem.getName()).matches()) {
            validationErrorBuildItemProducer.produce(new ValidationErrorBuildItem(
                    new IllegalArgumentException(
                            "Invalid application name '%s' - it should match '%s'".formatted(applicationInfoBuildItem.getName(),
                                    ApplicationNaming.UPPER_CAMEL_CASE_PATTERN.pattern()))));
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
    void datasourceConfiguration(final ApplicationInfoBuildItem applicationInfoBuildItem,
                                 final BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
                                 final BuildProducer<ContentBuildItem> contentBuildItemBuildProducer) {
        try {
            final SchemaName databaseSchema = SchemaName.from(new ApplicationNaming(applicationInfoBuildItem.getName()));

            final Map<String, String> datasourceConfiguration = Map.of("quarkus.datasource.jdbc.additional-jdbc-properties.currentSchema",
                    databaseSchema.name(), "quarkus.datasource.jdbc.max-size", "100");
            datasourceConfiguration.forEach((key, value) -> runTimeConfigurationDefaultBuildItemBuildProducer.produce(
                    new RunTimeConfigurationDefaultBuildItem(key, value)));

            contentBuildItemBuildProducer.produce(new ContentBuildItem(new Title(Title.Level.SECOND, "Datasource configuration")));
            contentBuildItemBuildProducer.produce(new ContentBuildItem(CodeBlock.fromProperties(datasourceConfiguration)));
        } catch (final IllegalStateException exception) {
            // Do nothing will be caught later
        }
    }

    @BuildStep
    ComposeServiceBuildItem generateCompose() {
        return ComposeProcessor.POSTGRES_COMPOSE_SERVICE_BUILD_ITEM;
    }

    /*
    2025-11-30 15:24:58,424 ERROR [io.qua.ver.htt.run.QuarkusErrorHandler] (executor-thread-1) HTTP Request to /prise_de_commande/1/ajouterPlat failed, error id: 3d488f63-b3a7-41e9-a9e5-b3d179b96e4e-1: com.damdamdeo.pulse.extension.core.encryption.DecryptionException: org.bouncycastle.openpgp.PGPException: cannot create cipher: No such provider: BC
        at com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService.decrypt(OpenPGPDecryptionService.java:68)
        at com.damdamdeo.pulse.extension.common.runtime.encryption.OpenPGPDecryptionService_ClientProxy.decrypt(Unknown Source)
        at com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresEventRepository.loadOrderByVersionASC(JdbcPostgresEventRepository.java:134)
        at com.damdamdeo.pulse.extension.core.command.CommandHandler.lambda$handle$0(CommandHandler.java:30)
     */
    @BuildStep
    BouncyCastleProviderBuildItem bouncyCastleProviderBuildItemProducer() {
        return new BouncyCastleProviderBuildItem();
    }
}
