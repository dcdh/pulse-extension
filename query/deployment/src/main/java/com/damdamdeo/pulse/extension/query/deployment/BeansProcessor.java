package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.query.GenericQuery;
import com.damdamdeo.pulse.extension.core.query.file.DefaultUploadedAtProvider;
import com.damdamdeo.pulse.extension.core.query.file.FileSizeLimitedCopier;
import com.damdamdeo.pulse.extension.core.query.file.query.DownloadQuery;
import com.damdamdeo.pulse.extension.core.query.file.query.GetFileInfoQuery;
import com.damdamdeo.pulse.extension.core.query.file.query.GetTraceByFileIdentifierQuery;
import com.damdamdeo.pulse.extension.core.query.file.query.UploadQuery;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DefaultDownloadedAtProvider;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DefaultTokenGenerator;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedAtProvider;
import com.damdamdeo.pulse.extension.query.runtime.JdbcPostgresExecutedByResolver;
import com.damdamdeo.pulse.extension.query.runtime.QueryExceptionMapper;
import com.damdamdeo.pulse.extension.query.runtime.SmallryeConfigBackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.query.runtime.file.*;
import com.damdamdeo.pulse.extension.query.runtime.file.filigrane.*;
import com.damdamdeo.pulse.extension.query.runtime.file.traceability.*;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.JdbcPostgresOwnedByProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

import java.util.List;

public class BeansProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(SmallryeConfigBackendUserVisibilityRolesProvider.class,
                        JdbcPostgresExecutedByResolver.class, JdbcPostgresOwnedByProvider.class)
                .build();
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem additionalIndexedClasses() {
        return new AdditionalIndexedClassesBuildItem(QueryExceptionMapper.class.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem registerDefaultUploadedAtProvider() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(DefaultUploadedAtProvider.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerFiligrane() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(
                        JpegAwtImageContentTypeFiligraneApplier.class,
                        JpgAwtImageContentTypeFiligraneApplier.class,
                        PdfBoxContentTypeFiligraneApplier.class,
                        PngAwtImageContentTypeFiligraneApplier.class,
                        DefaultFiligraneApplier.class)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerJdbcPostgresFileRepository() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(JdbcPostgresFileRepository.class).build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerTikaImageMetadataExtractor() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(TikaImageMetadataExtractor.class).build();
    }

    @BuildStep
    void registerFileDownloaderEndpoint(final BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItemBuildProducer,
                                        final BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalIndexedClassesBuildItemBuildProducer.produce(new AdditionalIndexedClassesBuildItem(FileEndpoint.class.getName()));
        additionalBeanBuildItemBuildProducer.produce(AdditionalBeanBuildItem.builder().addBeanClasses(FileEndpoint.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build());
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem registerFileParamConverterProvider() {
        return new AdditionalIndexedClassesBuildItem(FileParamConverterProvider.class.getName());
    }

    @BuildStep
    AdditionalBeanBuildItem registerDefaultTokenGenerator() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(DefaultTokenGenerator.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerDownloadedAtProvider() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(DownloadedAtProvider.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerTokenApplier() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(TokenApplierProducer.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerJdbcPostgresTokenRepository() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(JdbcPostgresTokenRepository.class).build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerTokenAppliers() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(
                        JpegContentTypeTokenApplier.class,
                        PdfContentTypeTokenApplier.class,
                        PngContentTypeTokenApplier.class)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerDefaultDownloadedAtProvider() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(DefaultDownloadedAtProvider.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerUploadQuery() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(UploadQuery.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerFileSizeLimitedCopier() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(FileSizeLimitedCopier.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerDownloadQuery() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(DownloadQuery.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerGetFileInfoQuery() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(GetFileInfoQuery.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerGetTraceByFileIdentifierQuery() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(GetTraceByFileIdentifierQuery.class)
                .setUnremovable()
                .setDefaultScope(DotNames.APPLICATION_SCOPED)
                .build();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> registerGenericQuery(final CombinedIndexBuildItem combinedIndexBuildItem) {
        return combinedIndexBuildItem.getIndex().getAllKnownImplementations(GenericQuery.class)
                .stream()
                .map(genericQuery -> AdditionalBeanBuildItem.builder()
                        .addBeanClass(genericQuery.name().toString())
                        .setDefaultScope(DotNames.APPLICATION_SCOPED)
                        .setUnremovable()
                        .build())
                .toList();
    }

    @BuildStep
    AdditionalBeanBuildItem registerDefaultFileMetadataEncryption() {
        return new AdditionalBeanBuildItem(DefaultFileMetadataEncryption.class);
    }

    @BuildStep
    AdditionalBeanBuildItem registerDefaultCustomMetadataEncryption() {
        return new AdditionalBeanBuildItem(DefaultCustomMetadataEncryption.class);
    }
}
