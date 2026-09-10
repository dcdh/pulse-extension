package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.traceability.DetailedInvolvedFinder;
import jakarta.ws.rs.Path;

@Path("/traceability/finder/detailed")
public class DetailedInvolvedFinderEndpoint {

    private final DetailedInvolvedFinder detailedInvolvedFinder;

    public DetailedInvolvedFinderEndpoint(DetailedInvolvedFinder detailedInvolvedFinder) {
        this.detailedInvolvedFinder = detailedInvolvedFinder;
    }

    /*
    Page<DetailedInvolved> findBy(AggregateId aggregateId, Pagination pagination) throws FinderException;
    Page<DetailedInvolved> findBy(ExecutedByHashed executedByHashed, Pagination pagination) throws FinderException;
     */

}
