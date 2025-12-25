package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface OwnedByExecutedByEncoder {

    ExecutedByEncoder executedByEncoder(OwnedBy ownedBy);
}