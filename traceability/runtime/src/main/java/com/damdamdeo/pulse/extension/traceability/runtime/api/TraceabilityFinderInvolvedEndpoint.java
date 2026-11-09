package com.damdamdeo.pulse.extension.traceability.runtime.api;

import com.damdamdeo.pulse.extension.common.runtime.pagination.PaginationDTO;
import com.damdamdeo.pulse.extension.core.connectionidentifier.AnyIdentifiable;
import com.damdamdeo.pulse.extension.core.consumer.AnyAggregateId;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.pagination.Page;
import com.damdamdeo.pulse.extension.core.traceability.FinderException;
import com.damdamdeo.pulse.extension.core.traceability.IncludeUncompounded;
import com.damdamdeo.pulse.extension.core.traceability.Involved;
import com.damdamdeo.pulse.extension.core.traceability.InvolvedFinder;
import com.damdamdeo.pulse.extension.core.traceability.nboftimes.*;
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

    @Schema(description = "Actor involved in the execution of an identifiable.")
    public record InvolvedDTO(

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Identifier of the identifiable.", required = true)
            Identifiable identifiable,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Link to aggregate root identifier.", required = true)
            AnyAggregateId linkToAggregateRootId,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Hashed identifier of the actor who executed the operation.", required = true)
            ExecutedByHashed executedByHashed,

            @Schema(type = SchemaType.STRING, implementation = String.class,
                    description = "Information identifying the actor who executed the operation.", required = true)
            ExecutedBy executedBy,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Number of times the actor has been involved on executing successfully a command.", required = true)
            CommandNbOfTimes commandNbOfTimes,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Number of times the actor has been involved on an unauthorize execution of a command.", required = true)
            CommandUnauthorizedNbOfTimes commandUnauthorizedNbOfTimes,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Number of times the actor has been involved on a business failing execution of a command.", required = true)
            CommandBusinessFailedNbOfTimes commandBusinessFailedNbOfTimes,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Number of times the actor has been involved on executing a query.", required = true)
            QueryNbOfTimes queryNbOfTimes,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Number of times the actor has been involved on an unauthorized execution of a query.", required = true)
            QueryUnauthorizedNbOfTimes queryUnauthorizedNbOfTimes,

            @Schema(type = SchemaType.NUMBER, implementation = Integer.class,
                    description = "Number of times the actor has been involved on accessing an aggregate from the traceability.", required = true)
            TraceabilityNbOfTimes traceabilityNbOfTimes) {

        public InvolvedDTO {
            Objects.requireNonNull(identifiable);
            Objects.requireNonNull(linkToAggregateRootId);
            Objects.requireNonNull(executedByHashed);
            Objects.requireNonNull(executedBy);
            Objects.requireNonNull(commandNbOfTimes);
            Objects.requireNonNull(commandUnauthorizedNbOfTimes);
            Objects.requireNonNull(commandBusinessFailedNbOfTimes);
            Objects.requireNonNull(queryNbOfTimes);
            Objects.requireNonNull(queryUnauthorizedNbOfTimes);
            Objects.requireNonNull(traceabilityNbOfTimes);
        }

        public InvolvedDTO(final Involved involved) {
            this(involved.identifiable(),
                    involved.linkTo(),
                    involved.actor().executedByHashed(), involved.actor().executedBy(),
                    involved.commandNbOfTimes(),
                    involved.commandUnauthorizedNbOfTimes(),
                    involved.commandBusinessFailedNbOfTimes(),
                    involved.queryNbOfTimes(),
                    involved.queryUnauthorizedNbOfTimes(),
                    involved.traceabilityNbOfTimes());
        }
    }

    @Path("byIdentifiable/{identifiable}")
    @GET
    public InvolvedPageDTO findBy(@PathParam("identifiable") final AnyIdentifiable identifiable,
                                  @QueryParam("includeUncompounded") @DefaultValue("false") final IncludeUncompounded includeUncompounded,
                                  @BeanParam final PaginationDTO paginationDTO) throws FinderException {
        Objects.requireNonNull(identifiable);
        Objects.requireNonNull(includeUncompounded);
        Objects.requireNonNull(paginationDTO);
        final Page<Involved> by = involvedFinder.findBy(identifiable, includeUncompounded, paginationDTO.toPagination());
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
