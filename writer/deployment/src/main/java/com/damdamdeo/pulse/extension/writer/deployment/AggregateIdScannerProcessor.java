package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.writer.deployment.items.AggregateIdBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import java.util.List;

public class AggregateIdScannerProcessor {

    @BuildStep
    List<AggregateIdBuildItem> discoverAggregatesId(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(AggregateId.class)
                .stream().map(aggregateIdClassInfo -> {
                    try {
                        final Class<? extends AggregateId> aggregateIdClazz =
                                (Class<? extends AggregateId>)
                                        classLoader.loadClass(
                                                aggregateIdClassInfo.name().toString());
                        return new AggregateIdBuildItem(aggregateIdClazz);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .distinct()
                .toList();
    }
}
