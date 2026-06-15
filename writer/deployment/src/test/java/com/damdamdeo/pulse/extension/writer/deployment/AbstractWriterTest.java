package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractWriterTest {

    @ApplicationScoped
    static class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
            return Optional.empty();
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
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
}
