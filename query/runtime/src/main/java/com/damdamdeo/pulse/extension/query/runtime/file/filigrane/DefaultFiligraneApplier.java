package com.damdamdeo.pulse.extension.query.runtime.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.FiligraneApplier;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import com.damdamdeo.pulse.extension.core.query.file.UnsupportedContentTypeException;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import io.quarkus.arc.All;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class DefaultFiligraneApplier implements FiligraneApplier {

    @Inject
    @All
    List<ContentTypeFiligraneApplier> contentTypeFiligraneAppliers;

    @Inject
    PulseQueryConfig pulseQueryConfig;

    @Override
    public FileContent apply(final FileContent fileContent) throws UnableToApplyFiligraneException {
        Objects.requireNonNull(fileContent);
        final ContentTypeFiligraneApplier applier = contentTypeFiligraneAppliers.stream()
                .filter(contentTypeFiligraneApplier -> fileContent.contentType().equals(contentTypeFiligraneApplier.contentType()))
                .findFirst()
                .orElseThrow(() -> new UnableToApplyFiligraneException(new UnsupportedContentTypeException()));
        return applier.apply(fileContent, pulseQueryConfig.file().filigrane()
                .orElse("Unknown filigrane, please define one."));
    }
}
