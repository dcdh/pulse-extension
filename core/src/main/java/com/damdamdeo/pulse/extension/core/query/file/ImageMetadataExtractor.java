package com.damdamdeo.pulse.extension.core.query.file;

import java.io.InputStream;

public interface ImageMetadataExtractor {

    FileMetadata extract(InputStream content, ContentType contentType) throws ImageMetadataExtractorException;
}
