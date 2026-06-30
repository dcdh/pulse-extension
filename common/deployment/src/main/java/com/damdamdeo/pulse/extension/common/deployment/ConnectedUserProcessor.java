package com.damdamdeo.pulse.extension.common.deployment;

import com.damdamdeo.pulse.extension.common.runtime.connecteduser.ConnectedUserNotAvailableUserProvider;
import com.damdamdeo.pulse.extension.common.runtime.connecteduser.QuarkusOidcConnectedUserProvider;
import com.damdamdeo.pulse.extension.core.BusinessException;
import com.damdamdeo.pulse.extension.core.connecteduser.DefaultConnectedUserFacade;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import jakarta.transaction.Transactional;
import org.jboss.jandex.*;

import java.util.ArrayList;
import java.util.List;

public class ConnectedUserProcessor {

    @BuildStep
    List<AdditionalBeanBuildItem> produceAdditionalBeanBuildItem(final Capabilities capabilities) {
        final List<AdditionalBeanBuildItem> additionalBeanBuildItems = new ArrayList<>();
        if (capabilities.isPresent(Capability.OIDC)) {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(QuarkusOidcConnectedUserProvider.class)
                    .build());
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(DefaultConnectedUserFacade.class)
                    .setDefaultScope(DotNames.APPLICATION_SCOPED)
                    .setUnremovable()
                    .build());
        } else {
            additionalBeanBuildItems.add(AdditionalBeanBuildItem.builder()
                    .addBeanClasses(ConnectedUserNotAvailableUserProvider.class)
                    .build());
        }
        return additionalBeanBuildItems;
    }

    @BuildStep
    AnnotationsTransformerBuildItem addTransactionalToUseCases(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        final AnnotationValue valueBusinessException = AnnotationValue.createClassValue("", Type.create(
                DotName.createSimple(BusinessException.class),
                Type.Kind.CLASS
        ));
        // RuntimeException likes TechnicalException are always rolled back.
        // Not needed to add it to rollbackOn
        final AnnotationValue populatedRollbackOn = AnnotationValue.createArrayValue(
                "rollbackOn",
                new AnnotationValue[]{valueBusinessException});
        final AnnotationInstance transactionalAnnotation = AnnotationInstance.create(
                DotName.createSimple(Transactional.class),
                null, // target
                new AnnotationValue[]{populatedRollbackOn});
        return new AnnotationsTransformerBuildItem(
                (AnnotationTransformation) context -> {
                    final Declaration target = context.declaration();
                    if (target.kind() == AnnotationTarget.Kind.CLASS && target.asClass().name().equals(DotName.createSimple(DefaultConnectedUserFacade.class))) {
                        context.add(transactionalAnnotation);
                    }
                }
        );
    }
}
