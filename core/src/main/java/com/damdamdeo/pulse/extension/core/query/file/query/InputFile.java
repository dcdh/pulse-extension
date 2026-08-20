package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.ContentLength;
import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.Filename;

import java.io.InputStream;
import java.util.Objects;

public record InputFile(FileIdentifier fileIdentifier,
                        ContentLength contentLength,
                        InputStream content,
                        Filename filename,
                        OwnedBy ownedBy,
                        CustomMetadata customMetadata) {

    public InputFile {
        Objects.requireNonNull(fileIdentifier);
        Objects.requireNonNull(contentLength);
        Objects.requireNonNull(content);
        Objects.requireNonNull(filename);
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(customMetadata);
    }

    public ContentType contentType() {
        return filename.contentType();
    }
}
