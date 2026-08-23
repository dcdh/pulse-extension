package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;

import java.util.List;

public interface ContentTypeTokenApplier {

    FileContent apply(FileContent fileContent, Token token) throws UnableToApplyTokenException;

    List<ContentType> contentTypes();
}
