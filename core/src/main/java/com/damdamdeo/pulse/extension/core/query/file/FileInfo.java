package com.damdamdeo.pulse.extension.core.query.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.query.CustomMetadata;

import java.util.Objects;

public record FileInfo(FileIdentifier fileIdentifier,
                       Filename filename,
                       ContentType contentType,
                       ContentLength contentLength,
                       UploadedAt uploadedAt,
                       UploadedBy uploadedBy,
                       OwnedBy ownedBy,
                       FileMetadata fileMetadata,
                       CustomMetadata customMetadata) {

    public FileInfo {
        Objects.requireNonNull(fileIdentifier);
        Objects.requireNonNull(filename);
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(contentLength);
        Objects.requireNonNull(uploadedAt);
        Objects.requireNonNull(uploadedBy);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(fileMetadata);
        Objects.requireNonNull(customMetadata);
    }
}
