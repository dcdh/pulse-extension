package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.query.BackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.core.query.ExecutedByResolver;
import com.damdamdeo.pulse.extension.core.query.UnableToResolveException;
import com.damdamdeo.pulse.extension.core.usecase.DomainUseCase;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseExceptionCode;
import com.damdamdeo.pulse.extension.core.usecase.audience.Audience;
import com.damdamdeo.pulse.extension.core.usecase.audience.Everyone;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DomainUseCaseTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(CommandHandlerTest.DuplicateTodoException.class))
            .withConfigurationResource("application.properties");

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubBackendUserVisibilityRolesProvider implements BackendUserVisibilityRolesProvider {

        @Override
        public List<String> provide() {
            throw new IllegalStateException("Should not be called");
        }
    }

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubExecutedByResolver implements ExecutedByResolver {

        @Override
        public Set<ExecutedBy> resolve(final Set<AggregateId> aggregatesId) throws UnableToResolveException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public Set<ExecutedBy> resolve(final OwnedBy ownedBy) throws UnableToResolveException {
            throw new IllegalStateException("Should not be called");
        }
    }

    static class NoAudienceCreateTodoDomainUseCase implements DomainUseCase<TodoId, CreateTodo, Todo> {

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws UseCaseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public List<Audience> audiences() {
            return List.of();
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

    static class CreateTodoDomainUseCase implements DomainUseCase<TodoId, CreateTodo, Todo> {

        @Inject
        CommandHandler<Todo, TodoId> commandHandler;

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws UseCaseException {
            Objects.requireNonNull(givenCreateTodo);
            try {
                return commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                        CommandHandlerTest.DuplicateTodoException::new);
            } catch (final CommandException exception) {
                throw new UseCaseException(new IllegalStateException("should not be called"), UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
            }
        }

        @Override
        public List<Audience> audiences() {
            return List.of(Everyone.INSTANCE);
        }
    }

    static class UseCaseExceptionTodoDomainUseCase implements DomainUseCase<TodoId, CreateTodo, Todo> {

        @Inject
        CommandHandler<Todo, TodoId> commandHandler;

        @Override
        public Todo execute(final CreateTodo givenCreateTodo) throws UseCaseException {
            try {
                commandHandler.handle(sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber), givenCreateTodo,
                        CommandHandlerTest.DuplicateTodoException::new);
                throw new UseCaseException(new RuntimeException("Something wrong happened"), UseCaseExceptionCode.INFRASTRUCTURE_FAILURE);
            } catch (final CommandException exception) {
                throw new IllegalStateException("should not be called");
            }
        }

        @Override
        public List<Audience> audiences() {
            return List.of(Everyone.INSTANCE);
        }
    }
}
