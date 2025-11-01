package com.damdamdeo.pulse.extension.runtime.consumer;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.AggregateRootType;
import com.damdamdeo.pulse.extension.core.LastAggregateVersion;
import com.damdamdeo.pulse.extension.core.consumer.*;
import com.damdamdeo.pulse.extension.core.encryption.DecryptedPayload;
import com.damdamdeo.pulse.extension.core.encryption.DecryptionService;
import com.damdamdeo.pulse.extension.core.encryption.EncryptedPayload;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@Singleton
@Unremovable
@DefaultBean
public final class PostgresAggregateRootLoader implements AggregateRootLoader<JsonNode> {

    private final DataSource dataSource;
    private final DecryptionService decryptionService;
    private final ObjectMapper objectMapper;

    public PostgresAggregateRootLoader(final DataSource dataSource,
                                       final DecryptionService decryptionService,
                                       final ObjectMapper objectMapper) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.decryptionService = Objects.requireNonNull(decryptionService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public AggregateRootLoaded<JsonNode> getByApplicationNamingAndAggregateRootTypeAndAggregateId(final ApplicationNaming applicationNaming,
                                                                                                  final AggregateRootType aggregateRootType,
                                                                                                  final AggregateId aggregateId)
            throws UnknownAggregateRootException, AggregateRootLoaderException {
        Objects.requireNonNull(applicationNaming);
        Objects.requireNonNull(aggregateRootType);
        Objects.requireNonNull(aggregateId);
        try (final Connection connection = dataSource.getConnection()) {
            connection.setSchema(applicationNaming.value().toLowerCase());
            try (final PreparedStatement ps = connection.prepareStatement(
                    // language=sql
                    """
                            SELECT last_version, aggregate_root_payload, owned_by, in_relation_with FROM t_aggregate_root
                            WHERE aggregate_root_type = ? AND aggregate_root_id = ?
                            """)) {
                ps.setString(1, aggregateRootType.type());
                ps.setString(2, aggregateId.id());
                try (final ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final LastAggregateVersion lastAggregateVersion = new LastAggregateVersion(rs.getInt("last_version"));
                        final EncryptedPayload encryptedPayload = new EncryptedPayload(rs.getBytes("aggregate_root_payload"));
                        final OwnedBy ownedBy = new OwnedBy(rs.getString("owned_by"));
                        final DecryptedPayload decryptedPayload = decryptionService.decrypt(encryptedPayload, ownedBy);
                        final InRelationWith inRelationWith = new InRelationWith(rs.getString("in_relation_with"));
                        return new AggregateRootLoaded<>(
                                aggregateRootType,
                                aggregateId,
                                lastAggregateVersion,
                                encryptedPayload,
                                objectMapper.readTree(decryptedPayload.payload()),
                                ownedBy,
                                inRelationWith);
                    }
                    throw new UnknownAggregateRootException(aggregateRootType, aggregateId);
                } catch (final IOException e) {
                    throw new AggregateRootLoaderException(aggregateRootType, aggregateId, e);
                }
            }
        } catch (final SQLException e) {
            throw new AggregateRootLoaderException(aggregateRootType, aggregateId, e);
        }
    }
}
