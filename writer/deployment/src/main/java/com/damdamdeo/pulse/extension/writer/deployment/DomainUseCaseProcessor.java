package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.ConnectionIdentifierProcessor;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.AbstractRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.core.connecteduser.update.AbstractUpdateUserNameUseCase;
import com.damdamdeo.pulse.extension.core.usecase.AbstractCreationalDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.AbstractDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.GuardDomainUseCase;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

import java.util.ArrayList;
import java.util.List;

public class DomainUseCaseProcessor {

    @BuildStep
    List<AdditionalIndexedClassesBuildItem> indexDomainUseCaseClasses(final Capabilities capabilities) {
        final List<Class<?>> classes = new ArrayList<>();
        classes.add(GuardDomainUseCase.class);
        classes.add(AbstractCreationalDomainUseCase.class);
        classes.add(AbstractDomainUseCase.class);
        if (capabilities.isPresent(Capability.OIDC) && ConnectionIdentifierProcessor.hasQuarkusJdbcPostgresInClassPath.get()) {
            classes.add(AbstractRegistrationDomainUseCase.class);
            classes.add(AbstractUpdateUserNameUseCase.class);
        }
        return classes.stream().map(clazz -> new AdditionalIndexedClassesBuildItem(clazz.getName())).toList();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerUseCase(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex().getAllKnownImplementations(DomainUseCase.class)
                .stream()
                .map(useCase -> AdditionalBeanBuildItem.builder()
                        .addBeanClass(useCase.name().toString())
                        .setDefaultScope(DotNames.SINGLETON)
                        .setUnremovable()
                        .build())
                .toList();
    }
}
