package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.encryption.Encrypted;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionException;
import com.damdamdeo.pulse.extension.core.encryption.EncryptionService;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.*;
import com.damdamdeo.pulse.extension.core.query.GenericQuery;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.QueryExceptionCode;
import com.damdamdeo.pulse.extension.core.query.file.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

// This must be call internally in reaction of an event representing a file.
// The content and other information must be passed in Command.
public final class UploadQuery implements GenericQuery<InputFile, FileIdentifier> {

    public static final Logger LOGGER = LoggerFactory.getLogger(UploadQuery.class);

    private final FileRepository fileRepository;
    private final ExecutionContextProvider executionContextProvider;
    private final EncryptionService encryptionService;
    private final ImageMetadataExtractor imageMetadataExtractor;
    private final UploadedAtProvider uploadedAtProvider;
    private final FileSizeLimitedCopier fileSizeLimitedCopier;
    private final UsernameEncoder usernameEncoder;
    private final FileMetadataEncryption fileMetadataEncryption;
    private final CustomMetadataEncryption customMetadataEncryption;

    public UploadQuery(final FileRepository fileRepository,
                       final ExecutionContextProvider executionContextProvider,
                       final EncryptionService encryptionService,
                       final ImageMetadataExtractor imageMetadataExtractor,
                       final UploadedAtProvider uploadedAtProvider,
                       final FileSizeLimitedCopier fileSizeLimitedCopier,
                       final UsernameEncoder usernameEncoder,
                       final FileMetadataEncryption fileMetadataEncryption,
                       final CustomMetadataEncryption customMetadataEncryption) {
        this.fileRepository = Objects.requireNonNull(fileRepository);
        this.executionContextProvider = Objects.requireNonNull(executionContextProvider);
        this.encryptionService = Objects.requireNonNull(encryptionService);
        this.imageMetadataExtractor = Objects.requireNonNull(imageMetadataExtractor);
        this.uploadedAtProvider = Objects.requireNonNull(uploadedAtProvider);
        this.fileSizeLimitedCopier = Objects.requireNonNull(fileSizeLimitedCopier);
        this.usernameEncoder = Objects.requireNonNull(usernameEncoder);
        this.fileMetadataEncryption = Objects.requireNonNull(fileMetadataEncryption);
        this.customMetadataEncryption = Objects.requireNonNull(customMetadataEncryption);
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
            fileSizeLimitedCopier.copy(inputFile.content(), temp, ContentLength.MAX.contentLength());
            if (!inputFile.contentLength().contentLength().equals(Files.size(temp))) {
                LOGGER.warn("Tmp file size does not match uploaded file size, tmp file size will be used.");
            }
            final ContentLength contentLength = new ContentLength(Files.size(temp));
            try (final InputStream metadata = Files.newInputStream(temp);
                 final InputStream encryption = Files.newInputStream(temp)) {
                final FileMetadata extracted = imageMetadataExtractor.extract(
                        metadata,
                        inputFile.contentType());
                final OwnedBy ownedBy = inputFile.ownedBy();
                final Encrypted<InputStream> encrypted = encryptionService.encrypt(encryption, inputFile.ownedBy(), t -> t);
                final ExecutedBy executedBy = executionContextProvider.provide().executedBy();
                final ExecutedByEncoded executedByEncodedUploadedBy = executedBy.encode(usernameEncoder, ownedBy);
                final UploadedAt uploadedAt = uploadedAtProvider.now();
                fileRepository.store(
                        new EncryptedFileInfo(
                                inputFile.fileIdentifier(),
                                inputFile.filename(),
                                inputFile.contentType(),
                                contentLength,
                                uploadedAt,
                                new EncryptedUploadedBy(executedByEncodedUploadedBy, ownedBy),
                                ownedBy,
                                fileMetadataEncryption.encrypt(extracted, ownedBy),
                                customMetadataEncryption.encrypt(inputFile.customMetadata(), ownedBy)
                        ), encrypted);
                return inputFile.fileIdentifier();
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (final UnableToEncodeException | FileAlreadyUploadedException exception) {
            throw new QueryException(exception, QueryExceptionCode.CONFLICT);
        } catch (final MaxFileSizeReachedException exception) {
            throw new QueryException(exception, QueryExceptionCode.FAIL_FAST_CONDITION_NOT_MET);
        } catch (final FileRepositoryException | EncryptionException | ImageMetadataExtractorException
                       | MetadataEncryptionException | UnableToCopyException | IOException exception) {
            throw new QueryException(exception, QueryExceptionCode.INFRASTRUCTURE_FAILURE);
        }
    }
}
