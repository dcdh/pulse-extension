package com.damdamdeo.pulse.extension.build.report.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.content.BasicTable;
import com.damdamdeo.pulse.extension.build.report.deployment.content.TableRow;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Title;
import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
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
    void generate(final ApplicationInfoBuildItem applicationInfoBuildItem,
                  final List<ContentBuildItem> contentBuildItems,
                  final OutputTargetBuildItem outputTargetBuildItem) {
        final ApplicationNaming applicationNaming = ApplicationNaming.of(applicationInfoBuildItem.getName());
        final String applicationVersion = applicationInfoBuildItem.getVersion();
        final String generated = AsciiDoctorGenerator.generate(
                Stream.concat(Stream.of(new Title(Title.Level.FIRST, "Build Report"),
                                        new BasicTable(
                                                List.of(
                                                        new TableRow(List.of("Application naming", applicationNaming.name())),
                                                        new TableRow(List.of("Functional domain", applicationNaming.functionalDomain())),
                                                        new TableRow(List.of("Application version", applicationVersion))))
                                ).map(ContentBuildItem::new),
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
