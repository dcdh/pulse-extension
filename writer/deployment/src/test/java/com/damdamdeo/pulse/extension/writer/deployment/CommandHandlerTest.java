package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CommandHandlerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    // https://github.com/quarkusio/quarkus/issues/19676
    @Inject
    @CacheName("passphrase")
    Cache cache;

    @Inject
    CommandHandler<Todo, TodoId> commandHandler;

    @Inject
    DataSource dataSource;

    @Test
    void shouldExecuteCommand() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo(new TodoId("Damien", 20L), "lorem ipsum");

        // When
        final Todo todoCreated = commandHandler.handle(givenCreateTodo);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(new TodoId("Damien", 20L)),
                () -> assertThat(todoCreated.description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoCreated.status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.important()).isEqualTo(Boolean.FALSE)
        );

        final int count = countEventsInEventStore();
        assertThat(count).isEqualTo(1);
    }

    private int countEventsInEventStore() {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT COUNT(*) AS count FROM t_event
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("count");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
