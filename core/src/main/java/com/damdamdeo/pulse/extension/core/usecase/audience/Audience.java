package com.damdamdeo.pulse.extension.core.usecase.audience;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.Prioritable;
import com.damdamdeo.pulse.extension.core.audience.AudienceExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;

public sealed interface Audience extends Prioritable permits Everyone, VisibilityRoleRestricted, InExecutedBy, ExecutedBySpecificServiceAccounts,
        ExecutedBySpecificEndUsers, ExecutedByHasAtLeastOneRole {

    boolean allow(AggregateId aggregateId, AudienceExecutionContext audienceExecutionContext) throws UseCaseException;

    boolean allow(AudienceExecutionContext audienceExecutionContext) throws UseCaseException;
}
