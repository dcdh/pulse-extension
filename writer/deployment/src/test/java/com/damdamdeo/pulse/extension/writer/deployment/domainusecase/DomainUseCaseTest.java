package com.damdamdeo.pulse.extension.writer.deployment.domainusecase;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.usecase.*;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;
import com.damdamdeo.pulse.extension.core.usecase.audience.Everyone;
import com.damdamdeo.pulse.extension.writer.deployment.AbstractWriterTest;
import com.damdamdeo.pulse.extension.writer.deployment.CommandHandlerTest;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DomainUseCaseTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            // classes.add(GuardDomainUseCase.class);
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(CommandHandlerTest.DuplicateTodoException.class,
                    StubBackendUserVisibilityRolesProvider.class, StubExecutedByResolver.class))
            .withConfigurationResource("application.properties");

    static class NoAudienceCreateTodoDomainUseCase extends AbstractCreationalDomainUseCase<TodoId, CreateTodo, Todo> {

        protected NoAudienceCreateTodoDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler) {
            super(commandHandler);
        }

        @Override
        public List<Audience> audiences() {
            return List.of();
        }

        @Override
        protected Function<SequenceNumber, TodoId> creational(final CreateTodo createTodo) {
            Objects.requireNonNull(createTodo);
            return sequenceNumber -> {
                throw new RuntimeException("Should not be called");
            };
        }

        @Override
        protected Function<TodoId, DuplicateAggregateException> duplicateAggregateException() {
            return todoId -> {
                throw new RuntimeException("Should not be called");
            };
        }
    }

    @Inject
    DataSource dataSource;

    @Inject
    CreateTodoDomainUseCase createTodoDomainUseCase;

    @Inject
    UseCaseExceptionTodoDomainUseCase useCaseExceptionTodoDomainUseCase;

    @Inject
    NoAudienceCreateTodoDomainUseCase noAudienceCreateTodoDomainUseCase;

    @Test
    void shouldFailWhenNoAudienceIsDefined() {
        assertThatThrownBy(() -> noAudienceCreateTodoDomainUseCase.execute(new CreateTodo("lorem ipsum")))
                .isExactlyInstanceOf(UseCaseException.class)
                .hasFieldOrPropertyWithValue("useCaseExceptionCode", UseCaseExceptionCode.FORBIDDEN)
                .cause()
                .isExactlyInstanceOf(UnauthorizedException.class);
    }

    @Order(1)
    @Test
    void shouldCreateTodo() throws UseCaseException {
        // Given

        // When
        final Todo loremIpsum = createTodoDomainUseCase.execute(new CreateTodo("lorem ipsum"));

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
        assertThatThrownBy(() -> useCaseExceptionTodoDomainUseCase.execute(new CreateTodo("lorem ipsum")))
                .isInstanceOf(UseCaseException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Something wrong happened");

        // Then
        assertThat(listEventsAggregateRootId(dataSource)).containsExactly("U000001-T000001");
    }

    static class CreateTodoDomainUseCase extends AbstractCreationalDomainUseCase<TodoId, CreateTodo, Todo> {

        protected CreateTodoDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler) {
            super(commandHandler);
        }

        @Override
        protected Function<SequenceNumber, TodoId> creational(final CreateTodo createTodo) {
            Objects.requireNonNull(createTodo);
            return sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber);
        }

        @Override
        protected Function<TodoId, DuplicateAggregateException> duplicateAggregateException() {
            return todoId -> {
                throw new IllegalStateException("Should not be called");
            };
        }

        @Override
        public List<Audience> audiences() {
            return List.of(Everyone.INSTANCE);
        }
    }

    static class UseCaseExceptionTodoDomainUseCase extends AbstractDomainUseCase<TodoId, CreateTodo, Todo> {

        protected UseCaseExceptionTodoDomainUseCase(final CommandHandler<Todo, TodoId> commandHandler) {
            super(commandHandler);
        }

        @Override
        protected CreateTodo onBefore(final CreateTodo command) throws UseCaseExecutionException {
            throw new UseCaseExecutionException(new RuntimeException("Something wrong happened"), UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
        }

        @Override
        public List<Audience> audiences() {
            return List.of(Everyone.INSTANCE);
        }

        @Override
        protected Supplier<MissingAggregateException> missingAggregateException() {
            throw new IllegalStateException("Should not be called");
        }
    }
}
