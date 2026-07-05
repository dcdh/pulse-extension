package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class AbstractWriterTest {

    @ApplicationScoped
    static class StubPassphraseRepository implements PassphraseRepository {

        private Map<OwnedBy, Passphrase> passphrases = new HashMap<>();

        @Override
        public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
            return Optional.ofNullable(passphrases.get(ownedBy));
        }

        @Override
        public List<RetrievedPassphrase> list(List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            passphrases.put(ownedBy, passphrase);
            return passphrase;
        }
    }

    protected List<String> listEventsAggregateRootId(final DataSource dataSource) {
        Objects.requireNonNull(dataSource);
        final List<String> aggregateRootIds = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT aggregate_root_id FROM event
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                aggregateRootIds.add(rs.getString("aggregate_root_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return aggregateRootIds;
    }

    protected List<String> listEventsAggregateRootIdAggregateRootTypeEventType(final DataSource dataSource) {
        Objects.requireNonNull(dataSource);
        final List<String> aggregateRootIds = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT aggregate_root_id, aggregate_root_type, event_type FROM event
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                aggregateRootIds.add(rs.getString("aggregate_root_id") + "|" + rs.getString("aggregate_root_type")
                        + "|" + rs.getString("event_type"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return aggregateRootIds;
    }
}
