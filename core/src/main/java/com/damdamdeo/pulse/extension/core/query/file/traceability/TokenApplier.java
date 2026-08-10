package com.damdamdeo.pulse.extension.core.query.file.traceability;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.FileContent;

public interface TokenApplier {

    FileContent apply(FileContent fileContent, OwnedBy ownedBy) throws UnableToApplyTokenException;
}
