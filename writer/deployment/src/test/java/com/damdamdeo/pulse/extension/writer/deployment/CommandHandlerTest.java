package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.command.Handled;
import com.damdamdeo.pulse.extension.core.event.ExecutedByEvent;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.event.VersionizedEvent;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubBackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubExecutedByResolver;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class CommandHandlerTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            // classes.add(GuardDomainUseCase.class);
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(
                    StubBackendUserVisibilityRolesProvider.class, StubExecutedByResolver.class))
            .withConfigurationResource("application.properties");

    @Inject
    CommandHandler<Todo, TodoId> commandHandler;

    @Inject
    DataSource dataSource;

    @Test
    void shouldExecuteCommand() throws CommandException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("lorem ipsum");

        // When
        final Handled<Todo, TodoId> todoCreated = commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(todoCreated.id()).isEqualTo(TodoId.USER_1_TODO_1),
                () -> assertThat(todoCreated.aggregateRoot().description()).isEqualTo("lorem ipsum"),
                () -> assertThat(todoCreated.aggregateRoot().status()).isEqualTo(Status.IN_PROGRESS),
                () -> assertThat(todoCreated.aggregateRoot().important()).isEqualTo(Boolean.FALSE),
                () -> assertThat(todoCreated.events()).containsExactly(
                        new VersionizedEvent<>(new AggregateVersion(0),
                                new ExecutedByEvent<>(new NewTodoCreated("lorem ipsum"), ExecutedBy.NotAvailable.INSTANCE)))
        );

        final int count = countEventsInEventStore();
        assertThat(count).isEqualTo(1);
    }

    private int countEventsInEventStore() {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(
                     // language=sql
                     """
                             SELECT COUNT(*) AS count FROM event
                             """);
             final ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("count");
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class DuplicateTodoException extends DuplicateAggregateException {

        private final TodoId todoId;

        public DuplicateTodoException(final TodoId todoId) {
            this.todoId = Objects.requireNonNull(todoId);
        }
    }
}
