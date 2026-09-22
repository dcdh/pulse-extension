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

@Path("/traceability/finder/involved")
@ApplicationScoped
@Unremovable
public class TraceabilityFinderInvolvedEndpoint {

    private final InvolvedFinder involvedFinder;

    public TraceabilityFinderInvolvedEndpoint(final InvolvedFinder involvedFinder) {
        this.involvedFinder = Objects.requireNonNull(involvedFinder);
    }

    @Schema(description = "Page of involved actors associated with aggregates.")
    public record InvolvedPageDTO(

            @Schema(description = "List of involved actors.", required = true)
            List<InvolvedDTO> listOfInvolved,

            @Schema(description = "Total number of pages.", required = true)
            int totalPages,

            @Schema(description = "Whether another page is available after the current page.", required = true)
            boolean hasNext,

            @Schema(description = "Whether a page is available before the current page.", required = true)
            boolean hasPrevious) {

        public InvolvedPageDTO {
            Objects.requireNonNull(listOfInvolved);
        }
    }

    @Schema(description = "Actor involved in the execution of an aggregate.")
    public record InvolvedDTO(

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Identifier of the aggregate.", required = true)
            AggregateId aggregateId,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Hashed identifier of the actor who executed the operation.", required = true)
            ExecutedByHashed executedByHashed,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Information identifying the actor who executed the operation.", required = true)
            ExecutedBy executedBy,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Nombre of times the actor has been involved on executing a command.", required = true)
            NbOfTimes commandNbOfTimes,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Nombre of times the actor has been involved on executing a query.", required = true)
            NbOfTimes queryNbOfTimes) {

        public InvolvedDTO {
            Objects.requireNonNull(aggregateId);
            Objects.requireNonNull(executedByHashed);
            Objects.requireNonNull(executedBy);
            Objects.requireNonNull(commandNbOfTimes);
        }

        public InvolvedDTO(final Involved involved) {
            this(involved.aggregateId(), involved.actor().executedByHashed(), involved.actor().executedBy(),
                    involved.commandNbOfTimes(), involved.queryNbOfTimes());
        }
    }

    @Path("byAggregateId/{aggregateId}")
    @GET
    public InvolvedPageDTO findBy(@PathParam("aggregateId") final AnyAggregateId aggregateId,
                                  @QueryParam("includeUncompounded") @DefaultValue("false") final IncludeUncompounded includeUncompounded,
                                  @BeanParam final PaginationDTO paginationDTO) throws FinderException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(includeUncompounded);
        Objects.requireNonNull(paginationDTO);
        final Page<Involved> by = involvedFinder.findBy(aggregateId, includeUncompounded, paginationDTO.toPagination());
        return new InvolvedPageDTO(
                by.content().stream().map(InvolvedDTO::new).toList(),
                by.totalPages(), by.hasNext(), by.hasPrevious());
    }

    @Path("byExecutedByHashed/{executedByHashed}")
    @GET
    public InvolvedPageDTO findBy(@PathParam("executedByHashed") final ExecutedByHashed executedByHashed,
                                  @BeanParam final PaginationDTO paginationDTO) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(paginationDTO);
        final Page<Involved> by = involvedFinder.findBy(executedByHashed, paginationDTO.toPagination());
        return new InvolvedPageDTO(
                by.content().stream().map(InvolvedDTO::new).toList(),
                by.totalPages(), by.hasNext(), by.hasPrevious());
    }
}
