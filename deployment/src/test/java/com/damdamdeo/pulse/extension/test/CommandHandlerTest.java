package com.damdamdeo.pulse.extension.test;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class CommandHandlerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsResource("init.sql"))
            .withConfigurationResource("application.properties");

    @Inject
    CommandHandler<Todo, TodoId> commandHandler;

    @Inject
    DataSource dataSource;

    @Test
    void shouldExecuteCommand() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo(TodoId.from(new UUID(0, 0)), "lorem ipsum");

        // When
        final Todo todoCreated = commandHandler.handle(givenCreateTodo);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(TodoId.from(new UUID(0, 0))),
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
