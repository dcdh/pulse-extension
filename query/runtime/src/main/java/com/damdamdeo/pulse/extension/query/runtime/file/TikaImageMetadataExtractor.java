package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileMetadata;
import com.damdamdeo.pulse.extension.core.query.file.ImageMetadataExtractor;
import com.damdamdeo.pulse.extension.core.query.file.ImageMetadataExtractorException;
import io.quarkus.arc.Unremovable;
import io.quarkus.tika.TikaMetadata;
import io.quarkus.tika.TikaParseException;
import io.quarkus.tika.TikaParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
@Unremovable
public class TikaImageMetadataExtractor implements ImageMetadataExtractor {

    @Inject
    TikaParser tikaParser;

    @Override
    public FileMetadata extract(final InputStream content, final ContentType contentType) throws ImageMetadataExtractorException {
        Objects.requireNonNull(content);
        Objects.requireNonNull(contentType);
        try {
            final TikaMetadata tikaMetadata = tikaParser.getMetadata(content, contentType.contentType());
            final Map<String, List<String>> metadata = new HashMap<>(tikaMetadata.getNames().size());
            for (final String name : tikaMetadata.getNames()) {
                metadata.put(name, tikaMetadata.getValues(name));
            }
            return new FileMetadata(metadata);
        } catch (final TikaParseException exception) {
            throw new ImageMetadataExtractorException(exception);
        }
    }
}
