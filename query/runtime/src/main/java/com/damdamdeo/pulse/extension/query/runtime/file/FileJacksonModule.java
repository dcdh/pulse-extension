package com.damdamdeo.pulse.extension.query.runtime.file;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.*;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedAt;
import com.damdamdeo.pulse.extension.core.query.file.traceability.DownloadedBy;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class FileJacksonModule extends SimpleModule {

    public FileJacksonModule() {
        super("FileJacksonModule");
        addSerializer(FileIdentifier.class, new FileValueObjectSerializer());
        addSerializer(Filename.class, new FileValueObjectSerializer());
        addSerializer(ContentLength.class, new FileValueObjectSerializer());
        addSerializer(UploadedAt.class, new FileValueObjectSerializer());
        addSerializer(OwnedBy.class, new FileValueObjectSerializer());
        addSerializer(UploadedBy.class, new FileValueObjectSerializer());
        addSerializer(Token.class, new FileValueObjectSerializer());
        addSerializer(DownloadedBy.class, new FileValueObjectSerializer());
        addSerializer(DownloadedAt.class, new FileValueObjectSerializer());
    }
}
