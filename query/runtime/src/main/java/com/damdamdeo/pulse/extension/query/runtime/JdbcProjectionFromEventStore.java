package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.UnableToProvidePassphraseException;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class JdbcProjectionFromEventStore<I extends Input, P extends Projection> implements ProjectionFromEventStore<I, P> {

    static final Logger LOGGER = Logger.getLogger(JdbcProjectionFromEventStore.class.getName());

    @Inject
    DataSource dataSource;

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Optional<Result<P>> findBy(final OwnedBy ownedBy, final AggregateId aggregateId, final I input, final SingleResultAggregateQuery<I> singleResultAggregateQuery) throws ProjectionException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(input);
        Objects.requireNonNull(singleResultAggregateQuery);
        try {
            final String query = singleResultAggregateQuery.query(passphraseProvider.provide(ownedBy), aggregateId, input);
            LOGGER.fine(query);
            final AggregateIdCollector collector = new AggregateIdCollector();
            final ObjectReader reader = objectMapper.reader().withAttribute(AggregateIdCollector.class, collector);
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement findByPreparedStatement = connection.prepareStatement(query);
                 final ResultSet projectionResultSet = findByPreparedStatement.executeQuery()) {
                if (projectionResultSet.next()) {
                    final String response = projectionResultSet.getString("response");
                    LOGGER.fine(response);
                    final P result = reader.readValue(response, getProjectionClass());
                    return Optional.of(Result.of(result, collector.aggregateId()));
                } else {
                    return Optional.empty();
                }
            } catch (final IOException | SQLException e) {
                throw new ProjectionException(ownedBy, aggregateId, e);
            }
        } catch (UnableToProvidePassphraseException e) {
            throw new ProjectionException(ownedBy, aggregateId, e);
        }
    }

    @Override
    public Result<P> findAll(final OwnedBy ownedBy, final I input, final MultipleResultAggregateQuery<I> multipleResultAggregateQuery) throws ProjectionException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(input);
        Objects.requireNonNull(multipleResultAggregateQuery);
        try {
            final String query = multipleResultAggregateQuery.query(passphraseProvider.provide(ownedBy), ownedBy, input);
            LOGGER.fine(query);
            final AggregateIdCollector collector = new AggregateIdCollector();
            final ObjectReader reader = objectMapper.reader().withAttribute(AggregateIdCollector.class, collector);
            final List<P> responses = new ArrayList<>();
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement findByPreparedStatement = connection.prepareStatement(query);
                 final ResultSet projectionResultSet = findByPreparedStatement.executeQuery()) {
                while (projectionResultSet.next()) {
                    final String response = projectionResultSet.getString("response");
                    LOGGER.fine(response);
                    responses.add(
                            reader.readValue(response, getProjectionClass()));
                }
            } catch (final IOException | SQLException e) {
                throw new ProjectionException(ownedBy, e);
            }
            return Result.of(responses, collector.aggregateId());
        } catch (UnableToProvidePassphraseException e) {
            throw new ProjectionException(ownedBy, e);
        }
    }

    abstract protected Class<P> getProjectionClass();
}
