package com.damdamdeo.pulse.extension.common.deployment;

import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.gizmo.ClassCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CodeGenerationWriter {

    public static void writeGeneratedClass(final ClassCreator classCreator, final OutputTargetBuildItem outputTargetBuildItem) {
        classCreator.writeTo((name, data) -> {
            final Path classGeneratedPath = outputTargetBuildItem.getOutputDirectory().resolve(name.substring(name.lastIndexOf("/") + 1) + ".class");
            try {
                if (Files.notExists(classGeneratedPath)) {
                    Files.createFile(classGeneratedPath);
                }
                Files.write(classGeneratedPath, data, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
