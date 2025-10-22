package com.damdamdeo.pulse.extension.core.projection;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

public interface MultipleResultAggregateQuery {
    String query(char[] passphrase, OwnedBy ownedBy);
}
