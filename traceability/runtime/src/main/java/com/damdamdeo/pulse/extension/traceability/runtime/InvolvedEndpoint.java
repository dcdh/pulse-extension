package com.damdamdeo.pulse.extension.traceability.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Path("/traceability/finder/involved")
public class InvolvedFinderEndpoint {

    private final InvolvedFinder involvedFinder;

    public InvolvedFinderEndpoint(final InvolvedFinder involvedFinder) {
        this.involvedFinder = Objects.requireNonNull(involvedFinder);
    }

    // FCK openapi et c'est du jaxrs donc il me faut le custom deserializer
    @Schema(description = "Page of involved actors associated with aggregates.")
    public record InvolvedPageDTO(

            @Schema(
                    description = "List of involved actors.",
                    required = true
            )
            List<InvolvedDTO> listOfInvolved,

            @Schema(
                    description = "Total number of pages.",
                    example = "3",
                    required = true
            )
            int totalPages,

            @Schema(
                    description = "Whether another page is available after the current page.",
                    example = "true",
                    required = true
            )
            boolean hasNext,

            @Schema(
                    description = "Whether a page is available before the current page.",
                    example = "false",
                    required = true
            )
            boolean hasPrevious) {

        public InvolvedPageDTO {
            Objects.requireNonNull(listOfInvolved);
        }
    }

    @Schema(description = "Actor involved in the execution of an aggregate.")
    public record InvolvedDTO(

            @Schema(
                    description = "Identifier of the aggregate.",
                    required = true)
            AggregateId aggregateId,

            @Schema(
                    description = "Hashed identifier of the actor who executed the operation.",
                    required = true
            )
            ExecutedByHashed executedByHashed,

            @Schema(
                    description = "Information identifying the actor who executed the operation.",
                    required = true
            )
            ExecutedBy executedBy) {

        public InvolvedDTO {
            Objects.requireNonNull(aggregateId);
            Objects.requireNonNull(executedByHashed);
            Objects.requireNonNull(executedBy);
        }

        public InvolvedDTO(final Involved involved) {
            this(involved.aggregateId(), involved.executedByHashed(), involved.executedBy());
        }
    }

    @Path("byAggregateId")
    public InvolvedPageDTO findBy(FCK final AggregateId aggregateId,
                                  @BeanParam final Pagination pagination) throws FinderException {
        putain cela ne vas pas marcher parce qu'il me faut le type aussi !
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(pagination);
        final Page<Involved> by = involvedFinder.findBy(aggregateId, pagination);
        return new InvolvedPageDTO(
                by.content().stream().map(InvolvedDTO::new).toList(),
                by.totalPages(),
                by.hasNext(),
                by.hasPrevious());
    }

    @Path("byExecutedByHashed")
    public InvolvedPageDTO findBy(FCK final ExecutedByHashed executedByHashed,
                                  @BeanParam final Pagination pagination) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(pagination);
    }
}
