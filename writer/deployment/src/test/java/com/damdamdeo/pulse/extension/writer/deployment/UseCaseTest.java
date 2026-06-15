package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.UseCase;
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
class UseCaseTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(CommandHandlerTest.DuplicateTodoException.class))
            .withConfigurationResource("application.properties");

    @Inject
    DataSource dataSource;

    @Inject
    CreateTodoUseCase createTodoUseCase;

    @Inject
    BusinessExceptionTodoUseCase businessExceptionTodoUseCase;

    @Inject
    TechnicalExceptionTodoUseCase technicalExceptionTodoUseCase;

    @Order(1)
    @Test
    void shouldCreateTodo() throws BusinessException {
        // Given

        // When
        final Todo loremIpsum = createTodoUseCase.execute(new CreateTodo("lorem ipsum"));

        // Then
        assertAll(
                () -> assertThat(loremIpsum).isEqualTo(new Todo(new TodoId(UserId.USER_1, TodoId.SEQUENCE_NUMBER_1), "lorem ipsum", Status.IN_PROGRESS, false)),
                () -> assertThat(listEventsAggregateRootId(dataSource)).containsExactly("U000001-T000001")
        );
    }

    @Order(2)
    @Test
    void shouldRollbackOnBusinessException() {
        // Given

        // When
        assertThatThrownBy(() -> businessExceptionTodoUseCase.execute(new CreateTodo("lorem ipsum")))
                .isInstanceOf(BusinessException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Something wrong happened");

        // Then
        assertThat(listEventsAggregateRootId(dataSource)).containsExactly("U000001-T000001");
    }

    @Order(3)
    @Test
    void shouldRollbackOnTechnicalException() {
        // Given

        // When
        assertThatThrownBy(() -> technicalExceptionTodoUseCase.execute(new CreateTodo("lorem ipsum")))
                .isInstanceOf(TechnicalException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Something wrong happened");

        // Then
        assertThat(listEventsAggregateRootId(dataSource)).containsExactly("U000001-T000001");
    }

    static class CreateTodoUseCase implements UseCase<TodoId, CreateTodo, Todo> {

        @Inject
        CommandHandler<Todo, TodoId> commandHandler;

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws BusinessException {
            Objects.requireNonNull(givenCreateTodo);
            return commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                    CommandHandlerTest.DuplicateTodoException::new);
        }
    }

    static class BusinessExceptionTodoUseCase implements UseCase<TodoId, CreateTodo, Todo> {

        @Inject
        CommandHandler<Todo, TodoId> commandHandler;

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws BusinessException {
            commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                    CommandHandlerTest.DuplicateTodoException::new);
            throw new BusinessException(new RuntimeException("Something wrong happened"));
        }
    }

    static class TechnicalExceptionTodoUseCase implements UseCase<TodoId, CreateTodo, Todo> {

        @Inject
        CommandHandler<Todo, TodoId> commandHandler;

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws BusinessException {
            commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                    CommandHandlerTest.DuplicateTodoException::new);
            throw new TechnicalException(new RuntimeException("Something wrong happened"));
        }
    }
}
