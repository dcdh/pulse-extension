package com.damdamdeo.pulse.extension.core.event;

/**
 * Across multiple aggregate - belongs to a kind of organization
 */
public interface Ownership {
    OwnedBy ownedBy();
}
