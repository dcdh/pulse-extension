package com.damdamdeo.pulse.extension.writer.runtime.projection;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.projection.*;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class JdbcProjectionFromEventStore<P extends Projection> implements ProjectionFromEventStore<P> {

    static final Logger LOGGER = Logger.getLogger(JdbcProjectionFromEventStore.class.getName());

    @Inject
    DataSource dataSource;

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Optional<P> findBy(final OwnedBy ownedBy, final AggregateId aggregateId, final SingleResultAggregateQuery singleResultAggregateQuery) throws ProjectionException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(singleResultAggregateQuery);
        final String query = singleResultAggregateQuery.query(passphraseProvider.provide(ownedBy), aggregateId);
        LOGGER.fine(query);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement findByPreparedStatement = connection.prepareStatement(query);
             final ResultSet projectionResultSet = findByPreparedStatement.executeQuery()) {
            if (projectionResultSet.next()) {
                final String response = projectionResultSet.getString("response");
                LOGGER.fine(response);
                return Optional.of(
                        objectMapper.readValue(response, getProjectionClass())
                );
            } else {
                return Optional.empty();
            }
        } catch (final JsonProcessingException | SQLException e) {
            throw new ProjectionException(ownedBy, aggregateId, e);
        }
    }

    @Override
    public List<P> findAll(final OwnedBy ownedBy, final MultipleResultAggregateQuery multipleResultAggregateQuery) throws ProjectionException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(multipleResultAggregateQuery);
        final String query = multipleResultAggregateQuery.query(passphraseProvider.provide(ownedBy), ownedBy);
        LOGGER.fine(query);
        final List<P> responses = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement findByPreparedStatement = connection.prepareStatement(query);
             final ResultSet projectionResultSet = findByPreparedStatement.executeQuery()) {
            while (projectionResultSet.next()) {
                final String response = projectionResultSet.getString("response");
                LOGGER.fine(response);
                responses.add(
                        objectMapper.readValue(response, getProjectionClass()));
            }
        } catch (final JsonProcessingException | SQLException e) {
            throw new ProjectionException(ownedBy, e);
        }
        return responses;
    }

    abstract protected Class<P> getProjectionClass();
}
