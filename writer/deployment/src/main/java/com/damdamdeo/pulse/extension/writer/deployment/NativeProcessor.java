package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRoot;
import com.damdamdeo.pulse.extension.core.command.Command;
import com.damdamdeo.pulse.extension.core.command.CreationalCommand;
import com.damdamdeo.pulse.extension.core.event.Event;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

import java.util.List;

public class NativeProcessor {

    @BuildStep
    List<ReflectiveHierarchyBuildItem> registerCommands(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Command.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveHierarchyBuildItem.builder(className).constructors(true)
                        .fields(true).methods(true).ignoreNested(false).build())
                .toList();
    }

    @BuildStep
    List<ReflectiveHierarchyBuildItem> registerCreationalCommands(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(CreationalCommand.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveHierarchyBuildItem.builder(className).constructors(true)
                        .fields(true).methods(true).ignoreNested(false).build())
                .toList();
    }

    @BuildStep
    List<ReflectiveHierarchyBuildItem> registerEvents(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(Event.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveHierarchyBuildItem.builder(className).constructors(true)
                        .fields(true).methods(true).ignoreNested(false).build())
                .toList();
    }

    @BuildStep
    List<ReflectiveHierarchyBuildItem> registerAggregateRoots(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(AggregateRoot.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveHierarchyBuildItem.builder(className).constructors(true)
                        .fields(true).methods(true).ignoreNested(false).build())
                .toList();
    }

    @BuildStep
    List<ReflectiveHierarchyBuildItem> registerAggregateIds(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex()
                .getAllKnownImplementations(AggregateId.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveHierarchyBuildItem.builder(className).constructors(true)
                        .fields(true).methods(true).ignoreNested(false).build())
                .toList();
    }
}
