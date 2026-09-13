package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.UserId;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.UseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GenericDomainUseCaseTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(CommandHandlerTest.DuplicateTodoException.class))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    CreateTodoUseCase createTodoGenericUseCase;

    @Inject
    UseCaseExceptionTodoUseCase useCaseExceptionTodoGenericUseCase;

    @Order(1)
    @Test
    void shouldCreateTodo() throws UseCaseException {
        // Given

        // When
        final Todo loremIpsum = createTodoGenericUseCase.execute(new CreateTodo("lorem ipsum"));

        // Then
        assertAll(
                () -> assertThat(loremIpsum).isEqualTo(new Todo(TodoId.USER_1_TODO_1, "lorem ipsum", Status.IN_PROGRESS, false)),
                () -> assertThat(listEventsAggregateRootId(dataSource)).containsExactly("U000001-T000001")
        );
    }

    @Order(2)
    @Test
    void shouldRollbackOnUseCaseException() {
        // Given

        // When
        assertThatThrownBy(() -> useCaseExceptionTodoGenericUseCase.execute(new CreateTodo("lorem ipsum")))
                .isInstanceOf(UseCaseException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Something wrong happened");

        // Then
        assertThat(listEventsAggregateRootId(dataSource)).containsExactly("U000001-T000001");
    }

    static class CreateTodoUseCase implements UseCase<CreateTodo, Todo> {

        @Inject
        CommandHandler<Todo, TodoId> commandHandler;

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws UseCaseException {
            Objects.requireNonNull(givenCreateTodo);
            try {
                return commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                        CommandHandlerTest.DuplicateTodoException::new);
            } catch (final CommandException exception) {
                throw new IllegalStateException("should not be called");
            }
        }
    }

    static class UseCaseExceptionTodoUseCase implements UseCase<CreateTodo, Todo> {

        @Inject
        CommandHandler<Todo, TodoId> commandHandler;

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws UseCaseException {
            try {
                commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                        CommandHandlerTest.DuplicateTodoException::new);
                throw new UseCaseException(new RuntimeException("Something wrong happened"));
            } catch (final CommandException exception) {
                throw new IllegalStateException("should not be called");
            }
        }
    }

}

