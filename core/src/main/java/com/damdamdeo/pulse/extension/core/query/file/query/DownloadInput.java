package com.damdamdeo.pulse.extension.core.query.file.query;

import com.damdamdeo.pulse.extension.core.query.Input;
import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;

import java.util.Objects;

public record DownloadInput(FileIdentifier fileIdentifier) implements Input {

    public DownloadInput {
        Objects.requireNonNull(fileIdentifier);
    }
}
