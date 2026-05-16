package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.writer.deployment.items.IdentifiableBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

import java.util.List;

public class IdentifiableScannerProcessor {

    @BuildStep
    List<IdentifiableBuildItem> discoverIdentifiables(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Identifiable.class)
                .stream().map(identifiableClassInfo -> {
                    try {
                        final Class<? extends Identifiable> identifiableClazz =
                                (Class<? extends Identifiable>)
                                        classLoader.loadClass(identifiableClassInfo.name().toString());
                        return new IdentifiableBuildItem(identifiableClazz);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .distinct()
                .toList();
    }

    @BuildStep
    List<IdentifiableBuildItem> discoverAggregatesId(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(AggregateId.class)
                .stream().map(aggregateIdClassInfo -> {
                    try {
                        final Class<? extends AggregateId> aggregateIdClazz =
                                (Class<? extends AggregateId>)
                                        classLoader.loadClass(
                                                aggregateIdClassInfo.name().toString());
                        return new IdentifiableBuildItem(aggregateIdClazz);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .distinct()
                .toList();
    }
}
