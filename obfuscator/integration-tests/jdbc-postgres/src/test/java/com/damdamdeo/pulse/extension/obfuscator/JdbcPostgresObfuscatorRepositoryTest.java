package com.damdamdeo.pulse.extension.obfuscator;

import com.damdamdeo.pulse.extension.core.obfuscator.UnableToDeObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnableToObfuscateException;
import com.damdamdeo.pulse.extension.core.obfuscator.UnknownObfuscatedException;
import com.damdamdeo.pulse.extension.obfuscator.runtime.JdbcPostgresObfuscatorRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@QuarkusTest
class JdbcPostgresObfuscatorRepositoryTest {

    @Inject
    JdbcPostgresObfuscatorRepository jdbcPostgresObfuscatorRepository;

    @Inject
    DataSource dataSource;

    @BeforeEach
    @AfterEach
    void tearDown() {
        try (final Connection connection = dataSource.getConnection();
             final Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE pulse.obfuscator");
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldStore() throws UnableToObfuscateException {
        // Given

        // When
        final UUID obfuscatedValue = jdbcPostgresObfuscatorRepository.store(new UUID(0, 0), "toObfuscate");

        // Then
        final List<String> storedObfuscated = new ArrayList<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                                 SELECT original, obfuscated
                                 FROM pulse.obfuscator
                             """);
             final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final String original = rs.getString("original");
                final String obfuscated = rs.getString("obfuscated");
                storedObfuscated.add(original + "|" + obfuscated);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        assertAll(
                () -> assertThat(obfuscatedValue).isEqualTo(new UUID(0, 0)),
                () -> assertThat(storedObfuscated).containsExactly("toObfuscate|00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void shouldReturnStored() throws UnableToObfuscateException {
        // Given
        jdbcPostgresObfuscatorRepository.store(new UUID(0, 0), "toObfuscate");

        // When
        final UUID obfuscatedValue = jdbcPostgresObfuscatorRepository.store(new UUID(0, 0), "toObfuscate");

        // Then
        assertThat(obfuscatedValue).isEqualTo(new UUID(0, 0));
    }

    @Test
    void shouldRetrieve() throws UnableToObfuscateException, UnknownObfuscatedException, UnableToDeObfuscateException {
        // Given
        jdbcPostgresObfuscatorRepository.store(new UUID(0, 0), "toObfuscate");

        // When
        final Optional<String> deObfuscate = jdbcPostgresObfuscatorRepository.retrieve(new UUID(0, 0));

        // Then
        assertThat(deObfuscate).isEqualTo(Optional.of("toObfuscate"));
    }

    @Test
    void shouldRetrieveReturnEmptyWhenNotPresent() throws UnableToDeObfuscateException {
        // Given

        // When
        final Optional<String> deObfuscate = jdbcPostgresObfuscatorRepository.retrieve(new UUID(0, 0));

        // Then
        assertThat(deObfuscate).isEqualTo(Optional.empty());
    }
}
