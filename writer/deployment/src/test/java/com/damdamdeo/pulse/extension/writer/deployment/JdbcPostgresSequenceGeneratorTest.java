package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
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
                () -> assertThat(sequences).containsExactly("todotaking_todo.seq_customfailingtodoid",
                        "todotaking_todo.seq_customidentifiable",
                        "todotaking_todo.seq_sampleidentifiable",
                        "todotaking_todo.seq_todochecklistid",
                        "todotaking_todo.seq_todoid",
                        "todotaking_todo.seq_useraggregateid",
                        "todotaking_todo.seq_userid"),
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
    void shouldGenerateSequenceInOrderFromBelongsTo() throws SequenceGenerationException {
        // Given
        final List<For<TodoChecklistId>> given = List.of(
                new For<>(TodoChecklistId.class, User.BELONGS_TO_USER_1_TODO_1),
                new For<>(TodoChecklistId.class, User.BELONGS_TO_USER_1_TODO_1),
                new For<>(TodoChecklistId.class, User.BELONGS_TO_USER_2_TODO_1));

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
                                 SELECT identifiable_clazz, belongs_to, next_value
                                 FROM sequence_by_identifiable_clazz_and_belongs_to
                                 ORDER BY identifiable_clazz, belongs_to;
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final String identifiableClazz = rs.getString("identifiable_clazz");
                final String ownedBy = rs.getString("belongs_to");
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
                        "TodoChecklistId|U000001-T000001|2", "TodoChecklistId|U000002-T000001|1"));
    }
}
