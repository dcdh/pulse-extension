package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.encryption.storage.runtime.DefaultPassphraseObfuscator;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.JdbcPostgresPassphraseRepository;
import com.damdamdeo.pulse.extension.encryption.storage.runtime.VaultPassphraseRepository;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;

import java.util.function.Supplier;

public class EncryptionStorageProcessor {

    public static final Supplier<Boolean> hasVaultInClassPath = () -> QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.vault.VaultKVSecretEngine");

    public static final Supplier<Boolean> hasQuarkusJdbcPostgresInClassPath = () -> QuarkusClassLoader.isClassPresentAtRuntime("io.quarkus.jdbc.postgresql.runtime.PostgreSQLAgroalConnectionConfigurer");

    // no capability exposed by vault yet ...
    // https://github.com/quarkiverse/quarkus-vault/pull/507
    // check on dependency presence instead, which is bad
    @BuildStep
    AdditionalBeanBuildItem additionalBeans(final LaunchModeBuildItem launchMode,
                                            final BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrorBuildItemBuildProducer) {
        final AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (hasVaultInClassPath.get()) {
            builder.addBeanClasses(VaultPassphraseRepository.class);
        } else if (hasQuarkusJdbcPostgresInClassPath.get()) {
            builder.addBeanClasses(JdbcPostgresPassphraseRepository.class, DefaultPassphraseObfuscator.class);
        } else if (!LaunchMode.TEST.equals(launchMode.getLaunchMode())) {
            validationErrorBuildItemBuildProducer.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                    new IllegalStateException("No passphrase repository found - please add io.quarkiverse.vault:quarkus-vault or io.quarkus:quarkus-jdbc-postgresql dependency")));
        }
        return builder.build();
    }
}
