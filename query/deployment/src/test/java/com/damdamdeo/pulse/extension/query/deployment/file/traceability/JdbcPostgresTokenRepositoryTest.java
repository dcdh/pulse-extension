package com.damdamdeo.pulse.extension.query.deployment.file.traceability;

import com.damdamdeo.pulse.extension.core.query.file.FileIdentifier;
import com.damdamdeo.pulse.extension.core.query.file.traceability.*;
import com.damdamdeo.pulse.extension.query.runtime.file.traceability.JdbcPostgresTokenRepository;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcPostgresTokenRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    JdbcPostgresTokenRepository jdbcPostgresTokenRepository;

    @Test
    @Order(1)
    void shouldStoreTraceability() throws TokenRepositoryException {
        // Given
        final Traceability givenTraceability = new Traceability(
                new Token(new UUID(0, 0)),
                new FileIdentifier("facture.jpeg"),
                new DownloadedBy("NA"),
                new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                        LocalTime.of(13, 46, 40), ZoneOffset.UTC))
        );

        // When
        jdbcPostgresTokenRepository.store(givenTraceability);

        // TODO checker contenu via la datasource
        // Then
        final List<String> expected = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("SELECT token, file_identifier, downloaded_by, downloaded_at FROM pulse.token_download ORDER BY downloaded_at ASC")) {
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    expected.add(resultSet.getString("token") + "|" + resultSet.getString("file_identifier") + "|" + resultSet.getString("downloaded_by") + "|" + resultSet.getObject("downloaded_at", OffsetDateTime.class));
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        assertThat(expected).containsExactly("00000000-0000-0000-0000-000000000000|facture.jpeg|NA|1970-01-12T13:46:40Z");
    }

    @Test
    @Order(2)
    void shouldFailToStoreOnSameToken() {
        // Given
        final Traceability givenTraceability = new Traceability(
                new Token(new UUID(0, 0)),
                new FileIdentifier("facture01.jpeg"),
                new DownloadedBy("NA"),
                new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                        LocalTime.of(13, 46, 40), ZoneOffset.UTC))
        );

        // When && Then
        assertThatThrownBy(() -> jdbcPostgresTokenRepository.store(givenTraceability))
                .isExactlyInstanceOf(TokenRepositoryException.class)
                .cause()
                .isExactlyInstanceOf(TokenAlreadyExistsException.class);
    }

    @Test
    @Order(3)
    void shouldFailToDelete() {
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 DELETE FROM pulse.token_download WHERE token = '00000000-0000-0000-0000-000000000000'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: Deletion of token_download is forbidden");
    }

    @Test
    @Order(4)
    void shouldFailToUpdate() {
        assertThatThrownBy(() -> {
            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement ps = connection.prepareStatement(
                         // language=sql
                         """
                                 UPDATE pulse.token_download SET file_identifier = 'overloaded'  WHERE token = '00000000-0000-0000-0000-000000000000'
                                 """);
                 final ResultSet rs = ps.executeQuery()) {
            }
        }).isExactlyInstanceOf(PSQLException.class)
                .hasMessageContaining("ERROR: Modification of token_download is forbidden");
    }

    @Test
    @Order(5)
    void shouldListByFileIdentifierOrderByDownloadedAtAsc() throws TokenRepositoryException {
        // Given
        final List<Traceability> givenListOfTraceability = List.of(
                new Traceability(
                        new Token(new UUID(0, 10)),
                        new FileIdentifier("facture01.jpeg"),
                        new DownloadedBy("NA"),
                        new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                                LocalTime.of(13, 46, 40), ZoneOffset.UTC))
                ),
                new Traceability(
                        new Token(new UUID(0, 11)),
                        new FileIdentifier("facture01.jpeg"),
                        new DownloadedBy("NA"),
                        new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                                LocalTime.of(13, 47, 40), ZoneOffset.UTC))
                ),
                new Traceability(
                        new Token(new UUID(0, 12)),
                        new FileIdentifier("facture01.jpeg"),
                        new DownloadedBy("NA"),
                        new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                                LocalTime.of(13, 48, 40), ZoneOffset.UTC))
                )
        );
        givenListOfTraceability.forEach(givenTraceability -> {
            try {
                jdbcPostgresTokenRepository.store(givenTraceability);
            } catch (final TokenRepositoryException exception) {
                throw new RuntimeException(exception);
            }
        });

        // When
        final List<Traceability> listOfTraceability = jdbcPostgresTokenRepository.listByFileIdentifierOrderByDownloadedAtAsc(new FileIdentifier("facture01.jpeg"));

        // Then
        assertThat(listOfTraceability).containsExactly(new Traceability(
                        new Token(new UUID(0, 10)),
                        new FileIdentifier("facture01.jpeg"),
                        new DownloadedBy("NA"),
                        new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                                LocalTime.of(13, 46, 40), ZoneOffset.UTC))
                ),
                new Traceability(
                        new Token(new UUID(0, 11)),
                        new FileIdentifier("facture01.jpeg"),
                        new DownloadedBy("NA"),
                        new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                                LocalTime.of(13, 47, 40), ZoneOffset.UTC))
                ),
                new Traceability(
                        new Token(new UUID(0, 12)),
                        new FileIdentifier("facture01.jpeg"),
                        new DownloadedBy("NA"),
                        new DownloadedAt(ZonedDateTime.of(LocalDate.of(1970, Month.JANUARY, 12),
                                LocalTime.of(13, 48, 40), ZoneOffset.UTC))
                ));
    }
}
