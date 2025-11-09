package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.writer.deployment.items.AggregateRootBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import java.util.Optional;

public class AggregateRootScannerProcessor {

    @BuildStep
    void discoverAggregates(final CombinedIndexBuildItem combinedIndexBuildItem,
                            final BuildProducer<AggregateRootBuildItem> aggregateRoots) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(AggregateRoot.class)
                .forEach(aggregateRootClassInfo -> {
                    final Type aggregateIdType = aggregateRootClassInfo.interfaceTypes().stream()
                            .filter(t -> t.name().equals(DotName.createSimple(AggregateRoot.class)))
                            .findFirst()
                            .flatMap(t -> t.kind() == Type.Kind.PARAMETERIZED_TYPE
                                    ? Optional.of(((ParameterizedType) t).arguments().get(0))
                                    : Optional.empty())
                            .orElseThrow(() -> new IllegalArgumentException("Should not be here"));

                    try {
                        aggregateRoots.produce(new AggregateRootBuildItem(
                                classLoader.loadClass(aggregateRootClassInfo.name().toString()),
                                classLoader.loadClass(aggregateIdType.toString())));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
