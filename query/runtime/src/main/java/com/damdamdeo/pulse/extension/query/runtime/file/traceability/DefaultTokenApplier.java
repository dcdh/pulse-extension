package com.damdamdeo.pulse.extension.query.runtime.file.traceability;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByEncoder;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.UnsupportedContentTypeException;
import com.damdamdeo.pulse.extension.core.query.file.traceability.*;
import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class DefaultTokenApplier implements TokenApplier {

    @Inject
    @All
    List<ContentTypeTokenApplier> contentTypeTokenAppliers;

    @Inject
    TokenGenerator tokenGenerator;

    @Inject
    TokenRepository tokenRepository;

    @Inject
    ExecutionContextProvider executionContextProvider;

    @Inject
    DownloadedAtProvider downloadedAtProvider;

    @Inject
    ExecutedByEncoder executedByEncoder;

    @Override
    public FileContent apply(final FileContent fileContent, final OwnedBy ownedBy) throws UnableToApplyTokenException {
        Objects.requireNonNull(fileContent);
        Objects.requireNonNull(ownedBy);
        try {
            final ContentTypeTokenApplier applier = contentTypeTokenAppliers.stream()
                    .filter(contentTypeTokenApplier -> contentTypeTokenApplier.contentTypes().contains(fileContent.contentType()))
                    .findFirst()
                    .orElseThrow(() -> new UnableToApplyTokenException(new UnsupportedContentTypeException()));
            final Token token = tokenGenerator.generate();
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final DownloadedBy downloadedBy = new DownloadedBy(executedBy.encode(executedByEncoder, ownedBy).encoded());
            final DownloadedAt downloadedAt = downloadedAtProvider.provide();
            tokenRepository.store(new Traceability(token, fileContent.id(), downloadedBy, downloadedAt));
            return applier.apply(fileContent, token);
        } catch (final UnableToEncodeException | TokenRepositoryException exception) {
            throw new UnableToApplyTokenException(exception);
        }
    }
}
