package com.damdamdeo.pulse.extension.query.runtime.file.filigrane;

import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.UnsupportedContentTypeException;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.FiligraneApplier;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;
import com.damdamdeo.pulse.extension.query.runtime.PulseQueryConfig;
import io.quarkus.arc.All;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
@Unremovable
public class DefaultFiligraneApplier implements FiligraneApplier {

    private final List<ContentTypeFiligraneApplier> contentTypeFiligraneAppliers;
    private final PulseQueryConfig pulseQueryConfig;
    private final ExecutionContextProvider executionContextProvider;

    public DefaultFiligraneApplier(@All final List<ContentTypeFiligraneApplier> contentTypeFiligraneAppliers,
                                   final PulseQueryConfig pulseQueryConfig,
                                   final ExecutionContextProvider executionContextProvider) {
        this.contentTypeFiligraneAppliers = Objects.requireNonNull(contentTypeFiligraneAppliers);
        this.pulseQueryConfig = Objects.requireNonNull(pulseQueryConfig);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
    }

    @Override
    public FileContent apply(final FileContent fileContent) throws UnableToApplyFiligraneException {
        Objects.requireNonNull(fileContent);
        final Optional<List<String>> roles = pulseQueryConfig.file().applyToHavingAtLeastOneRole();
        if (roles.isEmpty() || !executionContextProvider.provide().hasAnyRole(roles.get())) {
            return fileContent;
        }

        for (final ContentTypeFiligraneApplier applier : contentTypeFiligraneAppliers) {
            if (fileContent.contentType().equals(applier.contentType())) {
                return applier.apply(fileContent, pulseQueryConfig.file().filigrane()
                        .orElse("Unknown filigrane, please define one."));
            }
        }
        throw new UnableToApplyFiligraneException(new UnsupportedContentTypeException());
    }
}
