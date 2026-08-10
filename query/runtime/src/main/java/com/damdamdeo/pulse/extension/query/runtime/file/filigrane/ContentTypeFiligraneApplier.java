package com.damdamdeo.pulse.extension.query.runtime.file.filigrane;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.filigrane.UnableToApplyFiligraneException;

public interface ContentTypeFiligraneApplier {

    FileContent apply(FileContent fileContent, String text) throws UnableToApplyFiligraneException;

    ContentType contentType();
}
