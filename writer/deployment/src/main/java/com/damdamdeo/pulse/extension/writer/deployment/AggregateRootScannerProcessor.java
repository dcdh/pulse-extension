package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.writer.deployment.items.AggregateRootBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

public class AggregateRootScannerProcessor {

    @BuildStep
    void discoverAggregates(final CombinedIndexBuildItem combinedIndexBuildItem,
                            final BuildProducer<AggregateRootBuildItem> aggregateRoots) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(AggregateRoot.class)
                .forEach(aggregateRootClassInfo -> {
                    final Type superType = aggregateRootClassInfo.superClassType();
                    if (superType == null || superType.kind() != Type.Kind.PARAMETERIZED_TYPE) {
                        throw new IllegalArgumentException("AggregateRoot subclass must specify generic parameters");
                    }

                    final ParameterizedType parameterized = superType.asParameterizedType();
                    if (parameterized.arguments().isEmpty()) {
                        throw new IllegalArgumentException("AggregateRoot generic type K not found");
                    }

                    final Type aggregateIdType = parameterized.arguments().get(0);

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
