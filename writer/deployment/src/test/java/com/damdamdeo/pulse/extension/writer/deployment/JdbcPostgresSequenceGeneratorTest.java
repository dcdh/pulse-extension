package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
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
            .overrideRuntimeConfigKey("pulse.datasource.init-at-startup", "true")
            .withConfigurationResource("application.properties");

    @Inject
    JdbcPostgresSequenceGenerator jdbcPostgresSequenceGenerator;

    @Inject
    DataSource dataSource;

    record SampleIdentifiable() implements Identifiable {

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
                final String schema = rs.getString("sequence_schema");
                final String sequence = rs.getString("sequence_name");
                sequences.add(schema + "." + sequence);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        assertAll(
                () -> assertThat(sequences).containsExactly("todotaking_todo.seq_sampleidentifiable",
                        "todotaking_todo.seq_todochecklistid",
                        "todotaking_todo.seq_todoid",
                        "todotaking_todo.seq_useraggregateid"),
                () -> assertThat(sequenceNumber).isEqualTo(SequenceNumber.fromNumber(1L)));
    }

    @Test
    void shouldGenerateSequenceInOrder() {
        // Given
        final Class<? extends AggregateId> given = TodoChecklistId.class;

        // When && Then
        assertAll(
                () -> assertThat(jdbcPostgresSequenceGenerator.nextFor(given)).isEqualTo(SequenceNumber.fromNumber(1L)),
                () -> assertThat(jdbcPostgresSequenceGenerator.nextFor(given)).isEqualTo(SequenceNumber.fromNumber(2L))
        );
    }

    @Test
    void shouldGenerateSequenceInOrderFromOwner() throws SequenceGenerationException {
        // Given
        final List<For<TodoChecklistId>> given = List.of(
                new For<>(TodoChecklistId.class, new OwnedBy(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1).id())),
                new For<>(TodoChecklistId.class, new OwnedBy(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1).id())),
                new For<>(TodoChecklistId.class, new OwnedBy(new TodoId("Alban", TodoId.SEQUENCE_NUMBER_1).id())));

        // When
        final List<SequenceNumber> sequenceNumbers = new ArrayList<>();
        for (final For<TodoChecklistId> forTodoChecklistId : given) {
            final SequenceNumber sequenceNumber = jdbcPostgresSequenceGenerator.nextFor(forTodoChecklistId);
            sequenceNumbers.add(sequenceNumber);
        }

        final List<String> sequences = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT identifiable_clazz, owned_by, next_value
                                 FROM sequence_by_identifiable_clazz_and_owned_by
                                 ORDER BY identifiable_clazz, owned_by;
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final String identifiableClazz = rs.getString("identifiable_clazz");
                final String ownedBy = rs.getString("owned_by");
                final int nextValue = rs.getInt("next_value");
                sequences.add(identifiableClazz + "|" + ownedBy + "|" + nextValue);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        assertAll(
                () -> assertThat(sequenceNumbers).containsExactly(
                        SequenceNumber.fromNumber(1L),
                        SequenceNumber.fromNumber(2L),
                        SequenceNumber.fromNumber(1L)),
                () -> assertThat(sequences).containsExactly(
                        "TodoChecklistId|Alban-000001|1", "TodoChecklistId|Damien-000001|2"));
    }
}
