package com.damdamdeo.pulse.extension.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.Command;
import com.damdamdeo.pulse.extension.core.Event;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class NativeProcessor {

    @BuildStep
    ReflectiveClassBuildItem registerCommands(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final String[] classes = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Command.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);
        return ReflectiveClassBuildItem.builder(classes)
                .classes().constructors().fields().methods().publicConstructors().build();
    }

    @BuildStep
    ReflectiveClassBuildItem registerEvents(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final String[] classes = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Event.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);
        return ReflectiveClassBuildItem.builder(classes)
                .classes().constructors().fields().methods().publicConstructors().build();
    }

    @BuildStep
    ReflectiveClassBuildItem registerAggregateRoots(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final String[] classes = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(AggregateRoot.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);
        return ReflectiveClassBuildItem.builder(classes)
                .classes().constructors().fields().methods().publicConstructors().build();
    }

    @BuildStep
    ReflectiveClassBuildItem registerAggregateIds(final CombinedIndexBuildItem combinedIndexBuildItem) {
        final String[] classes = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(AggregateId.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);
        return ReflectiveClassBuildItem.builder(classes)
                .classes().constructors().fields().methods().publicConstructors().build();
    }
}
