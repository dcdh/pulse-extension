package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.UsernameEncoder;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.executedby.UnableToEncodeException;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.UnsupportedContentTypeException;

import java.util.List;
import java.util.Objects;

public final class TokenApplier {

    private final TokenGenerator tokenGenerator;
    private final TokenRepository tokenRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final DownloadedAtProvider downloadedAtProvider;
    private final UsernameEncoder usernameEncoder;
    private final List<ContentTypeTokenApplier> contentTypeTokenAppliers;

    public TokenApplier(final TokenGenerator tokenGenerator,
                        final TokenRepository tokenRepository,
                        final ExecutionContextProvider executionContextProvider,
                        final DownloadedAtProvider downloadedAtProvider,
                        final UsernameEncoder usernameEncoder,
                        final List<ContentTypeTokenApplier> contentTypeTokenAppliers) {
        this.tokenGenerator = Objects.requireNonNull(tokenGenerator);
        this.tokenRepository = Objects.requireNonNull(tokenRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.downloadedAtProvider = Objects.requireNonNull(downloadedAtProvider);
        this.usernameEncoder = Objects.requireNonNull(usernameEncoder);
        this.contentTypeTokenAppliers = Objects.requireNonNull(contentTypeTokenAppliers);
    }

    public FileContent apply(final FileContent fileContent, final OwnedBy ownedBy) throws TokenApplierException {
        Objects.requireNonNull(fileContent);
        Objects.requireNonNull(ownedBy);
        try {
            final ContentTypeTokenApplier applier = contentTypeTokenAppliers.stream()
                    .filter(contentTypeTokenApplier -> contentTypeTokenApplier.contentTypes().contains(fileContent.contentType()))
                    .findFirst()
                    .orElseThrow(() -> new UnableToApplyTokenException(new UnsupportedContentTypeException()));
            final Token token = tokenGenerator.generate();
            final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
            final EncryptedDownloadedBy encryptedDownloadedBy = new EncryptedDownloadedBy(executedBy.encode(usernameEncoder, ownedBy), ownedBy);
            final DownloadedAt downloadedAt = downloadedAtProvider.provide();
            tokenRepository.store(new EncryptedTraceability(token, fileContent.id(), encryptedDownloadedBy, downloadedAt));
            return applier.apply(fileContent, token);
        } catch (final TokenRepositoryException | UnableToEncodeException | UnableToApplyTokenException exception) {
            throw new TokenApplierException(exception);
        }
    }
}
