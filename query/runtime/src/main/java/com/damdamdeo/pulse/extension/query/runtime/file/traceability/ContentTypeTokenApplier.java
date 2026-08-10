package com.damdamdeo.pulse.extension.query.runtime.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.ContentType;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;
import com.damdamdeo.pulse.extension.core.query.file.traceability.Token;
import com.damdamdeo.pulse.extension.core.query.file.traceability.UnableToApplyTokenException;

import java.util.List;

public interface ContentTypeTokenApplier {

    FileContent apply(FileContent fileContent, Token token) throws UnableToApplyTokenException;

    List<ContentType> contentTypes();
}
