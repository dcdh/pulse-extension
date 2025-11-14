package com.damdamdeo.pulse.extension.core;

/**
 * Across multiple aggregate belonging to the same aggregateRoot, same aggregate root id.
 */
public interface Belonging {
    BelongsTo belongsTo();
}
