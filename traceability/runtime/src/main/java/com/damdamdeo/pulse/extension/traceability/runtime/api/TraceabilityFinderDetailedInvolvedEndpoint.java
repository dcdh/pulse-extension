package com.damdamdeo.pulse.extension.traceability.runtime.api;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.traceability.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Path("/traceability/finder/detailed")
@ApplicationScoped
@Unremovable
public class TraceabilityFinderDetailedInvolvedEndpoint {

    private final DetailedInvolvedFinder detailedInvolvedFinder;

    public TraceabilityFinderDetailedInvolvedEndpoint(final DetailedInvolvedFinder detailedInvolvedFinder) {
        this.detailedInvolvedFinder = Objects.requireNonNull(detailedInvolvedFinder);
    }

    @Schema(description = "Page of details involved actors associated with aggregates.")
    public record DetailedInvolvedPageDTO(

            @Schema(description = "List of involved actors.", required = true)
            List<DetailedInvolvedDTO> listOfInvolved,

            @Schema(description = "Total number of pages.", required = true)
            int totalPages,

            @Schema(description = "Whether another page is available after the current page.", required = true)
            boolean hasNext,

            @Schema(description = "Whether a page is available before the current page.", required = true)
            boolean hasPrevious) {

        public DetailedInvolvedPageDTO {
            Objects.requireNonNull(listOfInvolved);
        }
    }

    @Schema(description = "Actor involved in the execution of an aggregate.")
    public record DetailedInvolvedDTO(

            @Schema(type = SchemaType.NUMBER, implementation = Long.class,
                    description = "Trace id.", required = true)
            TraceId traceId,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Identifier of the aggregate.", required = true)
            AggregateId aggregateId,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Hashed identifier of the actor who executed the operation.", required = true)
            ExecutedByHashed executedByHashed,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Information identifying the actor who executed the operation.", required = true)
            ExecutedBy executedBy,

            @Schema(type = SchemaType.STRING, implementation = String.class, description = "From", required = true)
            From from,

            @Schema(type = SchemaType.STRING, implementation = String.class, description = "executedAt", required = true)
            ExecutedAt executedAt) {

        public DetailedInvolvedDTO {
            Objects.requireNonNull(traceId);
            Objects.requireNonNull(aggregateId);
            Objects.requireNonNull(executedByHashed);
            Objects.requireNonNull(executedBy);
            Objects.requireNonNull(from);
            Objects.requireNonNull(executedAt);
        }

        public DetailedInvolvedDTO(final DetailedInvolved detailedInvolved) {
            this(detailedInvolved.traceId(), detailedInvolved.involved().aggregateId(),
                    detailedInvolved.involved().executedByHashed(), detailedInvolved.involved().executedBy(),
                    detailedInvolved.from(), detailedInvolved.executedAt());
        }
    }

    @Path("byAggregateId/{aggregateId}")
    @GET
    public DetailedInvolvedPageDTO findBy(@PathParam("aggregateId") final AnyAggregateId aggregateId,
                                          @QueryParam("includeUncompounded") @DefaultValue("false") final IncludeUncompounded includeUncompounded,
                                          @BeanParam final PaginationDTO paginationDTO) throws FinderException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(includeUncompounded);
        Objects.requireNonNull(paginationDTO);
        final Page<DetailedInvolved> by = detailedInvolvedFinder.findBy(aggregateId, includeUncompounded, paginationDTO.toPagination());
        return new DetailedInvolvedPageDTO(
                by.content().stream().map(DetailedInvolvedDTO::new).toList(),
                by.totalPages(), by.hasNext(), by.hasPrevious());
    }

    @Path("byExecutedByHashed/{executedByHashed}")
    @GET
    public DetailedInvolvedPageDTO findBy(@PathParam("executedByHashed") final ExecutedByHashed executedByHashed,
                                          @BeanParam final PaginationDTO paginationDTO) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(paginationDTO);
        final Page<DetailedInvolved> by = detailedInvolvedFinder.findBy(executedByHashed, paginationDTO.toPagination());
        return new DetailedInvolvedPageDTO(
                by.content().stream().map(DetailedInvolvedDTO::new).toList(),
                by.totalPages(), by.hasNext(), by.hasPrevious());
    }
}
