package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionException;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutionContextProvider;
import com.damdamdeo.pulse.extension.core.query.GenericQuery;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.file.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

// This must be call internally in reaction of an event representing a file.
// The content and other information must be passed in Command.
public final class UploadQuery implements GenericQuery<InputFile, FileIdentifier> {

    private final FileRepository fileRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final EncryptionService encryptionService;
    private final ImageMetadataExtractor imageMetadataExtractor;
    private final UploadedAtProvider uploadedAtProvider;

    public UploadQuery(final FileRepository fileRepository,
                       final ExecutionContextProvider executionContextProvider,
                       final EncryptionService encryptionService,
                       final ImageMetadataExtractor imageMetadataExtractor,
                       final UploadedAtProvider uploadedAtProvider) {
        this.fileRepository = Objects.requireNonNull(fileRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.encryptionService = Objects.requireNonNull(encryptionService);
        this.imageMetadataExtractor = Objects.requireNonNull(imageMetadataExtractor);
        this.uploadedAtProvider = Objects.requireNonNull(uploadedAtProvider);
    }

    @Override
    public FileIdentifier execute(final InputFile inputFile) throws QueryException {
        Objects.requireNonNull(inputFile);
        try {
            inputFile.contentLength().checkValid();
            if (fileRepository.exists(inputFile.fileIdentifier())) {
                throw new FileAlreadyUploadedException();
            }
            final Path temp = Files.createTempFile("upload-", ".tmp");
            try (final InputStream in = inputFile.content()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            try (final InputStream metadata = Files.newInputStream(temp);
                 final InputStream encryption = Files.newInputStream(temp)) {
                final FileMetadata extracted = imageMetadataExtractor.extract(
                        metadata,
                        inputFile.contentType());
                final OwnedBy ownedBy = inputFile.ownedBy();
                final Encrypted<InputStream> encrypted = encryptionService.encrypt(encryption, inputFile.ownedBy(), t -> t);
                final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
                final UploadedAt uploadedAt = uploadedAtProvider.provide();
                fileRepository.store(
                        new FileInfo(
                                inputFile.fileIdentifier(),
                                inputFile.filename(),
                                inputFile.contentType(),
                                inputFile.contentLength(),
                                uploadedAt,
                                new UploadedBy(executedBy),
                                ownedBy,
                                extracted
                        ), encrypted);
                return inputFile.fileIdentifier();
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (final FileAlreadyUploadedException | FileRepositoryException | IOException |
                       EncryptionException | MaxFileSizeReachedException |
                       ImageMetadataExtractorException exception) {
            throw new QueryException(exception);
        }
    }
}
