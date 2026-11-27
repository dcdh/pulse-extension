package com.damdamdeo.pulse.extension.traceability.runtime.api;

import com.damdamdeo.pulse.extension.common.runtime.pagination.PaginationDTO;
import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AnyIdentifiable;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.EventType;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.traceability.*;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Path("/traceability/finder")
@ApplicationScoped
@Unremovable
public class TraceabilityInvolvedFinderEndpoint {

    private final InvolvedFinder involvedFinder;

    public TraceabilityInvolvedFinderEndpoint(final InvolvedFinder involvedFinder) {
        this.involvedFinder = Objects.requireNonNull(involvedFinder);
    }

    @Schema(description = "Execution details and counts for an identifiable.")
    public record AggregateIdInvolvedDTO(

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Identifier of the identifiable.", required = true)
            Identifiable identifiable,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Link to the aggregate root identifier.", required = true)
            AnyAggregateId linkToAggregateRootId,

            @Schema(description = "Page of execution details.", required = true)
            DetailedInvolvedPageDTO page,

            @Schema(description = "Execution counts by actor.", required = true)
            List<NbOfExecutionsDTO> nbOfExecutions) {

        public AggregateIdInvolvedDTO {
            Objects.requireNonNull(identifiable);
            Objects.requireNonNull(linkToAggregateRootId);
            Objects.requireNonNull(page);
            Objects.requireNonNull(nbOfExecutions);
        }

        public AggregateIdInvolvedDTO(final AggregateIdInvolved involved) {
            this(involved.identifiable(), involved.linkTo(), new DetailedInvolvedPageDTO(involved.page()),
                    involved.nbOfExecutions().stream().map(NbOfExecutionsDTO::new).toList());
        }
    }

    @Schema(description = "Execution details for an actor.")
    public record ExecutedByInvolvedDTO(

            @Schema(description = "Page of execution details.", required = true)
            DetailedInvolvedPageDTO page) {

        public ExecutedByInvolvedDTO {
            Objects.requireNonNull(page);
        }

        public ExecutedByInvolvedDTO(final ExecutedByInvolved involved) {
            this(new DetailedInvolvedPageDTO(involved.page()));
        }
    }

    @Schema(description = "Page of execution details.")
    public record DetailedInvolvedPageDTO(

            @Schema(description = "List of execution details.", required = true)
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

        public DetailedInvolvedPageDTO(final Page<DetailedInvolved> page) {
            this(page.content().stream().map(DetailedInvolvedDTO::new).toList(),
                    page.totalPages(), page.hasNext(), page.hasPrevious());
        }
    }

    @Schema(description = "Number of executions.")
    public record NbOfExecutionsDTO(

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Information identifying the actor who executed the operation.", required = true)
            ExecutedBy executedBy,

            @Schema(type = SchemaType.INTEGER, implementation = Integer.class,
                    description = "Command number of times.", required = true)
            CommandNbOfTimes commandNbOfTimes,

            @Schema(type = SchemaType.INTEGER, implementation = Integer.class,
                    description = "Command unauthorized number of times.", required = true)
            CommandUnauthorizedNbOfTimes commandUnauthorizedNbOfTimes,

            @Schema(type = SchemaType.INTEGER, implementation = Integer.class,
                    description = "Command business failed number of times.", required = true)
            CommandBusinessFailedNbOfTimes commandBusinessFailedNbOfTimes,

            @Schema(type = SchemaType.INTEGER, implementation = Integer.class,
                    description = "Query number of times.", required = true)
            QueryNbOfTimes queryNbOfTimes,

            @Schema(type = SchemaType.INTEGER, implementation = Integer.class,
                    description = "Query unauthorized number of times.", required = true)
            QueryUnauthorizedNbOfTimes queryUnauthorizedNbOfTimes,

            @Schema(type = SchemaType.INTEGER, implementation = Integer.class,
                    description = "Traceability number of times.", required = true)
            TraceabilityNbOfTimes traceabilityNbOfTimes) {

        public NbOfExecutionsDTO {
            Objects.requireNonNull(executedBy);
            Objects.requireNonNull(commandNbOfTimes);
            Objects.requireNonNull(commandUnauthorizedNbOfTimes);
            Objects.requireNonNull(commandBusinessFailedNbOfTimes);
            Objects.requireNonNull(queryNbOfTimes);
            Objects.requireNonNull(queryUnauthorizedNbOfTimes);
            Objects.requireNonNull(traceabilityNbOfTimes);
        }

        public NbOfExecutionsDTO(final NbOfExecutions counts) {
            this(counts.executedBy(), counts.commandNbOfTimes(), counts.commandUnauthorizedNbOfTimes(),
                    counts.commandBusinessFailedNbOfTimes(), counts.queryNbOfTimes(),
                    counts.queryUnauthorizedNbOfTimes(), counts.traceabilityNbOfTimes());
        }
    }

    @Schema(description = "Actor involved in the execution of an identifiable.")
    public record DetailedInvolvedDTO(

            @Schema(type = SchemaType.NUMBER, implementation = Long.class,
                    description = "Trace id.", required = true)
            TraceId traceId,

            @Schema(type = SchemaType.NUMBER, implementation = Long.class,
                    description = "Correlation id.", required = true)
            CorrelationId correlationId,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Hashed identifier of the actor who executed the operation.", required = true)
            ExecutedByHashed executedByHashed,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Information identifying the actor who executed the operation.", required = true)
            ExecutedBy executedBy,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Event type.", required = false)
            EventType eventType,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Aggregate version.", required = false)
            AggregateVersion aggregateVersion,

            @Schema(type = SchemaType.STRING, implementation = String.class, description = "Source", required = true)
            Source source,

            @Schema(type = SchemaType.STRING, implementation = String.class, description = "ExecutionStatus", required = true)
            ExecutionStatus executionStatus,

            @Schema(type = SchemaType.STRING, implementation = String.class, description = "From", required = true)
            From from,

            @Schema(type = SchemaType.STRING, implementation = String.class, description = "executedAt", required = true)
            ExecutedAt executedAt,

            @Schema(description = "Execution counts for this actor.", required = true)
            NbOfExecutionsDTO nbOfExecutions) {

        public DetailedInvolvedDTO {
            Objects.requireNonNull(traceId);
            Objects.requireNonNull(correlationId);
            Objects.requireNonNull(executedByHashed);
            Objects.requireNonNull(executedBy);
            Objects.requireNonNull(executionStatus);
            Objects.requireNonNull(source);
            Objects.requireNonNull(from);
            Objects.requireNonNull(executedAt);
            Objects.requireNonNull(nbOfExecutions);
        }

        public DetailedInvolvedDTO(final DetailedInvolved detailedInvolved) {
            this(detailedInvolved.traceId(), detailedInvolved.correlationId(),
                    detailedInvolved.actor().executedByHashed(), detailedInvolved.actor().executedBy(),
                    detailedInvolved.eventType(), detailedInvolved.aggregateVersion(),
                    detailedInvolved.source(), detailedInvolved.executionStatus(),
                    detailedInvolved.from(), detailedInvolved.executedAt(),
                    new NbOfExecutionsDTO(detailedInvolved.nbOfExecutions()));
        }
    }

    @Path("byIdentifiable/{identifiable}")
    @GET
    public AggregateIdInvolvedDTO findByIdentifiable(@PathParam("identifiable") final AnyIdentifiable identifiable,
                                                     @BeanParam final PaginationDTO paginationDTO) throws FinderException {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(paginationDTO);
        return new AggregateIdInvolvedDTO(involvedFinder.findBy(identifiable, paginationDTO.toPagination()));
    }

    @Path("byExecutedByHashed/{executedByHashed}")
    @GET
    public ExecutedByInvolvedDTO findByExecutedByHashed(@PathParam("executedByHashed") final ExecutedByHashed executedByHashed,
                                                        @BeanParam final PaginationDTO paginationDTO) throws FinderException {
        Objects.requireNonNull(executedByHashed);
        Objects.requireNonNull(paginationDTO);
        return new ExecutedByInvolvedDTO(involvedFinder.findBy(executedByHashed, paginationDTO.toPagination()));
    }
}
