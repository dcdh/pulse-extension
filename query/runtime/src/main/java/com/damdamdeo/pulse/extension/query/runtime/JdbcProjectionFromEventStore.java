package com.damdamdeo.pulse.extension.query.runtime;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseProvider;
import com.damdamdeo.pulse.extension.core.encryption.UnableToProvidePassphraseException;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.OwnedByProvider;
import com.damdamdeo.pulse.extension.query.runtime.ownedby.UnableToProvideOwnedByException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public abstract class JdbcProjectionFromEventStore<A extends AggregateId, P extends Projection<A>> implements ProjectionFromEventStore<A, P> {

    static final Logger LOGGER = Logger.getLogger(JdbcProjectionFromEventStore.class.getName());

    @Inject
    DataSource dataSource;

    @Inject
    PassphraseProvider passphraseProvider;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OwnedByProvider ownedByProvider;

    @Override
    public SingleResult<A, P> getOneByAggregateId(final A aggregateId, final SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(singleResultAggregateIdProjectionQuery);
        return findOneByAggregateId(aggregateId, singleResultAggregateIdProjectionQuery)
                .orElseThrow(() -> new ProjectionException(aggregateId, new UnknownProjectionException()));
    }

    @Override
    public Optional<SingleResult<A, P>> findOneByAggregateId(final A aggregateId, final SingleResultAggregateIdProjectionQuery singleResultAggregateIdProjectionQuery) throws ProjectionException {
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(singleResultAggregateIdProjectionQuery);
        try {
            final OwnedBy ownedBy = ownedByProvider.getByAggregateId(aggregateId);
            final String query = singleResultAggregateIdProjectionQuery.query(passphraseProvider.provide(ownedBy), aggregateId);
            LOGGER.fine(query);
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement findByPreparedStatement = connection.prepareStatement(query);
                 final ResultSet projectionResultSet = findByPreparedStatement.executeQuery()) {
                if (projectionResultSet.next()) {
                    final String response = projectionResultSet.getString("response");
                    LOGGER.fine(response);
                    final P result = objectMapper.readValue(response, getProjectionClass());
                    return Optional.of(new SingleResult<>(result));
                } else {
                    return Optional.empty();
                }
            } catch (final IOException | SQLException e) {
                throw new ProjectionException(aggregateId, e);
            }
        } catch (UnableToProvidePassphraseException | UnableToProvideOwnedByException e) {
            throw new ProjectionException(aggregateId, e);
        }
    }

    @Override
    public <I extends Input> MultipleResult<A, P> findAllBy(final OwnedBy ownedBy, final I input, final MultipleResultProjectionQuery<I> multipleResultProjectionQuery) throws ProjectionException {
        Objects.requireNonNull(ownedBy);
        Objects.requireNonNull(input);
        Objects.requireNonNull(multipleResultProjectionQuery);
        try {
            final String query = multipleResultProjectionQuery.query(passphraseProvider.provide(ownedBy), ownedBy, input);
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
            } catch (final IOException | SQLException e) {
                throw new ProjectionException(ownedBy, e);
            }
            return new MultipleResult<>(responses);
        } catch (UnableToProvidePassphraseException e) {
            throw new ProjectionException(ownedBy, e);
        }
    }

    abstract protected Class<A> getAggregateIdClass();

    abstract protected Class<P> getProjectionClass();
}
