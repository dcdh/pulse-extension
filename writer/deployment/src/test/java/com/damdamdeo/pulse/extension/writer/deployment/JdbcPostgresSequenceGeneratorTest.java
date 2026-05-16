package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.SequenceGenerationException;
import com.damdamdeo.pulse.extension.core.SequenceNumber;
import com.damdamdeo.pulse.extension.writer.runtime.JdbcPostgresSequenceGenerator;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JdbcPostgresSequenceGeneratorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false")
            .overrideConfigKey("quarkus.devservices.enabled", "true")
            .overrideConfigKey("pulse.datasource.init-at-startup", "true")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresSequenceGenerator jdbcPostgresSequenceGenerator;

    @Inject
    DataSource dataSource;

    record TodoId() implements AggregateId {

        @Override
        public String id() {
            throw new IllegalStateException("Should not be called");
        }
    }

    record TodoChecklistId() implements AggregateId {

        @Override
        public String id() {
            throw new IllegalStateException("Should not be called");
        }
    }

    @Test
    void shouldCreateSequences() throws SequenceGenerationException {
        // Given
        final Class<? extends AggregateId> given = TodoId.class;

        // When
        final SequenceNumber sequenceNumber = jdbcPostgresSequenceGenerator.nextFor(given);

        // Then
        final List<String> sequences = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT sequence_schema, sequence_name
                                 FROM information_schema.sequences
                                 ORDER BY sequence_schema, sequence_name;
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String schema = rs.getString("sequence_schema");
                String sequence = rs.getString("sequence_name");
                sequences.add(schema + "." + sequence);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        assertAll(
                () -> assertThat(sequences).containsExactly("todotaking_todo.seq_todochecklistid",
                        "todotaking_todo.seq_todoid", "todotaking_todo.seq_useraggregateid"),
                () -> assertThat(sequenceNumber).isEqualTo(SequenceNumber.fromNumber(1L)));
    }

    @Test
    void shouldGenerateSequenceInOrder() {
        final Class<? extends AggregateId> given = TodoChecklistId.class;

        assertAll(
                () -> assertThat(jdbcPostgresSequenceGenerator.nextFor(given)).isEqualTo(SequenceNumber.fromNumber(1L)),
                () -> assertThat(jdbcPostgresSequenceGenerator.nextFor(given)).isEqualTo(SequenceNumber.fromNumber(2L))
        );
    }
}
