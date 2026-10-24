package com.damdamdeo.pulse.extension.core.query.permission;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import com.damdamdeo.pulse.extension.core.query.QueryException;

// define a priority to avoid unnecessary computing.
// example: a Query has ROLE_RESTRICTED and PARTICIPANT role. ROLE_RESTRICATED is ultra-fast meanwhile IN_EXECUTED_BY
// will do a lot of processing, so ROLE-based must be checked first.
public sealed interface Permission<K extends AggregateId> permits CompositePermission {

    boolean allow(ProjectionId<K> projectionId, PermissionExecutionContext permissionExecutionContext) throws QueryException;
}
