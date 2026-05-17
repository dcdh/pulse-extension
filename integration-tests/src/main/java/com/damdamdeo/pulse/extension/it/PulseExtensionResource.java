/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.damdamdeo.pulse.extension.it;

import com.damdamdeo.pulse.extension.consumer.runtime.event.AsyncEventConsumerChannel;
import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.AddNewTodoItem;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.RegisterUser;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProvider;
import com.damdamdeo.pulse.extension.core.connecteduser.ConnectedUserAggregateIdProviderException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.UnableToFindException;
import io.quarkus.logging.Log;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Path("/pulse-extension")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class PulseExtensionResource {

    @Inject
    ConnectedUserAggregateIdProvider connectedUserAggregateIdProvider;

    @Inject
    CommandHandler<User, UserId> userCommandHandler;

    @Inject
    CommandHandler<Todo, TodoId> todoCommandHandler;

    @Inject
    CommandHandler<TodoChecklist, TodoChecklistId> todoChecklistCommandHandler;

    @Inject
    @Any
    Instance<StatisticsEventHandler> statisticsEventHandlerInstance;

    @Inject
    TodoProjectionQuery todoProjectionQuery;

    @Inject
    DataSource dataSource;

    public record CreationalWorkflowResponse(List<String> sequences,
                                             List<String> sequenceByIdentifiableClazzAndOwnedBy,
                                             List<String> databaseConnectionIdentifiers,
                                             List<String> aggregateRoots,
                                             List<String> events) {

        public CreationalWorkflowResponse {
            Objects.requireNonNull(sequences);
            Objects.requireNonNull(sequenceByIdentifiableClazzAndOwnedBy);
            Objects.requireNonNull(databaseConnectionIdentifiers);
            Objects.requireNonNull(aggregateRoots);
            Objects.requireNonNull(events);
        }
    }

    @POST
    @Path("/creationalWorkflow")
    @Transactional
    @Authenticated
    public Response creationalWorkflow() throws SQLException {
        try {
            final UserId provided = connectedUserAggregateIdProvider.provide(UserId.class, UserId::from, UserId::new);
            final User user = userCommandHandler.handle(provided, new RegisterUser(), DuplicateUserException::new);
            Log.info("User registered : " + user);

            final Todo todoCreated = todoCommandHandler.handle(sequenceNumber -> new TodoId(provided, sequenceNumber),
                    new CreateTodo("lorem ipsum"), DuplicateTodoException::new);
            Log.info("Todo created : " + todoCreated);

            final TodoChecklist todoChecklistAdded = todoChecklistCommandHandler.handle(sequenceNumber -> new TodoChecklistId(todoCreated.id(), sequenceNumber),
                    new AddNewTodoItem(todoCreated.id(), "Make it works !"), DuplicateTodoChecklistException::new);
            Log.info("TodoChecklist added : " + todoChecklistAdded);


            final List<String> sequences = new ArrayList<>();
            final List<String> sequenceByIdentifiableClazzAndBelongsTo = new ArrayList<>();
            final List<String> databaseConnectionIdentifiers = new ArrayList<>();
            final List<String> aggregateRoots = new ArrayList<>();
            final List<String> events = new ArrayList<>();
            try (final Connection connection = dataSource.getConnection()) {
                try (final PreparedStatement ps = connection.prepareStatement(
                        // language=sql
                        """
                                    SELECT sequence_schema, sequence_name
                                    FROM information_schema.sequences
                                    ORDER BY sequence_schema, sequence_name;
                                """);
                     final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String schema = rs.getString("sequence_schema");
                        final String sequence = rs.getString("sequence_name");
                        sequences.add(schema + "." + sequence);
                    }
                }
                try (final PreparedStatement ps = connection.prepareStatement(
                        // language=sql
                        """
                                    SELECT identifiable_clazz, belongs_to, next_value
                                    FROM sequence_by_identifiable_clazz_and_belongs_to
                                    ORDER BY identifiable_clazz, belongs_to;
                                """);
                     final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String identifiableClazz = rs.getString("identifiable_clazz");
                        final String belongsTo = rs.getString("belongs_to");
                        final int nextValue = rs.getInt("next_value");
                        sequenceByIdentifiableClazzAndBelongsTo.add(identifiableClazz + "|" + belongsTo + "|" + nextValue);
                    }
                }
                try (final PreparedStatement ps = connection.prepareStatement(
                        // language=sql
                        """
                                SELECT connection_identifier_hash AS connection_identifier_hash, identifiable_id AS identifiable_id FROM connection_identifier
                                """);
                     final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String connectionIdentifierHash = rs.getString("connection_identifier_hash");
                        final String identifiableId = rs.getString("identifiable_id");
                        databaseConnectionIdentifiers.add(connectionIdentifierHash + "|" + identifiableId);
                    }
                }
                try (final PreparedStatement ps = connection.prepareStatement(
                        // language=sql
                        """
                                SELECT aggregate_root_id, aggregate_root_type, last_version, aggregate_root_payload, owned_by, belongs_to FROM aggregate_root
                                """);
                     final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String aggregateRootId = rs.getString("aggregate_root_id");
                        final String aggregateRootType = rs.getString("aggregate_root_type");
                        final int lastVersion = rs.getInt("last_version");
                        final String ownedBy = rs.getString("owned_by");
                        final String belongsTo = rs.getString("belongs_to");
                        aggregateRoots.add(aggregateRootId + "|" + aggregateRootType + "|" + lastVersion + "|" + ownedBy + "|" + belongsTo);
                    }
                }
                try (final PreparedStatement ps = connection.prepareStatement(
                        // language=sql
                        """
                                SELECT aggregate_root_id, aggregate_root_type, version, stored_at, event_type, event_payload, owned_by, belongs_to, executed_by FROM event
                                """);
                     final ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String aggregateRootId = rs.getString("aggregate_root_id");
                        final String aggregateRootType = rs.getString("aggregate_root_type");
                        final int version = rs.getInt("version");
                        final String eventType = rs.getString("event_type");
                        final String ownedBy = rs.getString("owned_by");
                        final String belongsTo = rs.getString("belongs_to");
                        events.add(aggregateRootId + "|" + aggregateRootType + "|" + version + "|" + eventType + "|" + ownedBy + "|" + belongsTo);
                    }
                }
            }
            return Response.ok(new CreationalWorkflowResponse(sequences,
                    sequenceByIdentifiableClazzAndBelongsTo,
                    databaseConnectionIdentifiers,
                    aggregateRoots,
                    events)).build();
        } catch (final BusinessException | ConnectedUserAggregateIdProviderException exception) {
            return Response.serverError().entity(exception.getMessage()).build();
        }
    }

    @POST
    @Path("/called")
    public Call getCall() {
        final Call call = statisticsEventHandlerInstance
                .select(AsyncEventConsumerChannel.Literal.of("statistics")).get().getCall();
        return Optional.ofNullable(call)
                .orElseThrow(() -> new NotFoundException("Call not executed yet"));
    }

    @GET
    @Path("/listConnectedUserTodos")
    @Authenticated
    public Response listConnectedUserTodos() {
        try {
            final List<TodoProjection> todoProjections = todoProjectionQuery.getByConnectedUser();
            return Response.ok(todoProjections).build();
        } catch (final UnableToFindException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    static final class DuplicateUserException extends DuplicateAggregateException {

        private final UserId userId;

        public DuplicateUserException(final UserId userId) {
            this.userId = Objects.requireNonNull(userId);
        }
    }

    static final class DuplicateTodoException extends DuplicateAggregateException {

        private final TodoId todoId;

        public DuplicateTodoException(final TodoId todoId) {
            this.todoId = Objects.requireNonNull(todoId);
        }
    }

    static final class DuplicateTodoChecklistException extends DuplicateAggregateException {

        private final TodoChecklistId todoChecklistId;

        public DuplicateTodoChecklistException(final TodoChecklistId todoChecklistId) {
            this.todoChecklistId = Objects.requireNonNull(todoChecklistId);
        }
    }
}
