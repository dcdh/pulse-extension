package com.damdamdeo.pulse.extension.build.report.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

public class BuildReportProcessor {

    @BuildStep
    @Produce(ArtifactResultBuildItem.class)
    void generate(final List<ContentBuildItem> contentBuildItems,
                  final OutputTargetBuildItem outputTargetBuildItem) {
        final String generated = AsciiDoctorGenerator.generate(
                Stream.concat(Stream.of(new ContentBuildItem(new Title(Title.Level.FIRST, "Build Report"))),
                                contentBuildItems.stream())
                        .map(ContentBuildItem::content).toList());
        final Path generationReportPath = outputTargetBuildItem.getOutputDirectory().resolve("generationReport.adoc");
        try {
            if (Files.notExists(generationReportPath)) {
                Files.createFile(generationReportPath);
            }
            Files.writeString(generationReportPath, generated, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
