package com.damdamdeo.pulse.extension.core.query.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.FileContent;

public interface FiligraneApplier {

    FileContent apply(FileContent fileContent) throws UnableToApplyFiligraneException;
}
