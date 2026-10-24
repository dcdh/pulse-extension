package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.connecteduser.registration.AbstractRegistrationDomainUseCase;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.util.List;

public class RegistrationDomainUseCaseProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> produceAdditionalBeanBuildItem(final CombinedIndexBuildItem combinedIndexBuildItem,
                                                                 final Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.OIDC)) {
            return List.of();
        }
        final var index = combinedIndexBuildItem.getIndex();
        final var classLoader = Thread.currentThread().getContextClassLoader();
        return index.getAllKnownSubclasses(AbstractRegistrationDomainUseCase.class)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .map(className -> {
                    try {
                        return Class.forName(className, false, classLoader);
                    } catch (final ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(clazz -> AdditionalBeanBuildItem.builder()
                        .addBeanClass(clazz)
                        .setDefaultScope(DotNames.SINGLETON)
                        .setUnremovable()
                        .build()
                )
                .toList();
    }
}
