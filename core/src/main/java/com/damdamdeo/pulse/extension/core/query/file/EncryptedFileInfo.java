package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import org.apache.commons.lang3.Validate;

import java.util.Objects;

public record EncryptedFileInfo(FileIdentifier fileIdentifier,
                                Filename filename,
                                ContentType contentType,
                                ContentLength contentLength,
                                UploadedAt uploadedAt,
                                EncryptedUploadedBy encryptedUploadedBy,
                                OwnedBy ownedBy,
                                EncryptedFileMetadata encryptedFileMetadata,
                                EncryptedCustomMetadata encryptedCustomMetadata) {

    public EncryptedFileInfo {
        Objects.requireNonNull(fileIdentifier);
        Objects.requireNonNull(filename);
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(contentLength);
        Objects.requireNonNull(uploadedAt);
        Objects.requireNonNull(encryptedUploadedBy);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(encryptedFileMetadata);
        Objects.requireNonNull(encryptedCustomMetadata);
        Validate.validState(encryptedUploadedBy.ownedBy().equals(ownedBy));
    }
}
