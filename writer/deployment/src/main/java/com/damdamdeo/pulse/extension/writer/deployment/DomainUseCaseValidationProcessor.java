package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.common.deployment.items.ValidationErrorBuildItem;
import com.damdamdeo.pulse.extension.core.connecteduser.registration.AbstractRegistrationDomainUseCase;
import com.damdamdeo.pulse.extension.core.connecteduser.update.AbstractUpdateUserNameUseCase;
import com.damdamdeo.pulse.extension.core.usecase.AbstractCreationalDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.AbstractDomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.GuardDomainUseCase;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class DomainUseCaseValidationProcessor {

    private static final DotName DOMAIN_USE_CASE = DotName.createSimple(DomainUseCase.class);

    private static final Set<DotName> ALLOWED_DIRECT_IMPLEMENTATIONS = new LinkedHashSet<>();

    static {
        ALLOWED_DIRECT_IMPLEMENTATIONS.add(DotName.createSimple(AbstractDomainUseCase.class));
        ALLOWED_DIRECT_IMPLEMENTATIONS.add(DotName.createSimple(AbstractCreationalDomainUseCase.class));
        ALLOWED_DIRECT_IMPLEMENTATIONS.add(DotName.createSimple(AbstractRegistrationDomainUseCase.class));
        ALLOWED_DIRECT_IMPLEMENTATIONS.add(DotName.createSimple(AbstractUpdateUserNameUseCase.class));
        ALLOWED_DIRECT_IMPLEMENTATIONS.add(DotName.createSimple(GuardDomainUseCase.class));
    }

    @BuildStep
    void validateDomainUseCases(final CombinedIndexBuildItem combinedIndexBuildItem,
                                final BuildProducer<ValidationErrorBuildItem> validationErrors) {
        final Collection<ClassInfo> allKnownImplementations = combinedIndexBuildItem.getIndex().getAllKnownImplementations(DOMAIN_USE_CASE);
        for (final ClassInfo clazz : allKnownImplementations) {
            if (!directlyImplementsDomainUseCase(clazz)) {
                continue;
            }
            if (ALLOWED_DIRECT_IMPLEMENTATIONS.contains(clazz.name())) {
                continue;
            }
            validationErrors.produce(
                    new ValidationErrorBuildItem(
                            new IllegalStateException(
                                    "Illegal DomainUseCase implementation: %s. DomainUseCase may only be implemented by %s"
                                            .formatted(clazz.name(), ALLOWED_DIRECT_IMPLEMENTATIONS))));
        }
    }

    private static boolean directlyImplementsDomainUseCase(final ClassInfo clazz) {
        return clazz.interfaceTypes()
                .stream()
                .map(Type::name)
                .anyMatch(DOMAIN_USE_CASE::equals);
    }
}
