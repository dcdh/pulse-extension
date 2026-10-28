package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.traceability.TraceRecorderRepository;
import com.damdamdeo.pulse.extension.core.traceability.TraceRecorderRepositoryProvider;
import com.damdamdeo.pulse.extension.core.traceability.TraceabilityDetails;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
@Unremovable
public class ArcTraceRecorderRepositoryProvider implements TraceRecorderRepositoryProvider {

    @Inject
    @TraceRecorderQualifier(value = TraceRecorder.INVOLVED)
    TraceRecorderRepository traceRecorderRepositoryInvolved;

    @Inject
    @TraceRecorderQualifier(value = TraceRecorder.INVOLVED_WITH_FULL_DETAILS)
    Instance<TraceRecorderRepository> traceRecorderRepositoryInvolvedWithFullDetailsInstance;

    @Override
    public TraceRecorderRepository provide(final TraceabilityDetails traceabilityDetails) {
        return switch (traceabilityDetails) {
            case INVOLVED_ONLY -> traceRecorderRepositoryInvolved;
            case DETAILS_WHEN_AVAILABLE_OTHERWISE_INVOLVED_ONLY ->
                    traceRecorderRepositoryInvolvedWithFullDetailsInstance.isResolvable() ?
                            traceRecorderRepositoryInvolvedWithFullDetailsInstance.get() : traceRecorderRepositoryInvolved;
        };
    }
}
