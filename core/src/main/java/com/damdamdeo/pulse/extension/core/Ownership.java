package com.damdamdeo.pulse.extension.core;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;

/**
 * Across multiple aggregate - belongs to a kind of organization
 */
public interface Ownership {
    OwnedBy ownedBy();
}
