package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandException;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.listener.AbstractOnStoredEventListener;
import com.damdamdeo.pulse.extension.core.listener.OnStoredEventListener;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubBackendUserVisibilityRolesProvider;
import com.damdamdeo.pulse.extension.writer.deployment.domainusecase.StubExecutedByResolver;
import io.quarkus.arc.All;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OnStoredEventListenerTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            // classes.add(GuardDomainUseCase.class);
            .withApplicationRoot(javaArchive -> javaArchive.addClasses(CommandHandlerTest.DuplicateTodoException.class,
                    StubBackendUserVisibilityRolesProvider.class, StubExecutedByResolver.class))
            .withConfigurationResource("application.properties");

    @Inject
    CommandHandler<Todo, TodoId> commandHandler;

    @Inject
    @All
//    List<OnStoredEventListener<TodoId, Event<TodoId>>> onStoredEventListeners; not working ! will return empty
    List<OnStoredEventListener<TodoId, ?>> onStoredEventListeners;// It's ok

    Function<SequenceNumber, TodoId> creational = sequenceNumber -> new TodoId(UserId.USER_1, sequenceNumber);

    @Inject
    OnNewTodoCreated onNewTodoCreated;

    record On(TodoId id, AggregateVersion aggregateVersion, NewTodoCreated event) {
    }

    static class OnNewTodoCreated extends AbstractOnStoredEventListener<TodoId, NewTodoCreated> {

        final List<On> events = new ArrayList<>();

        @Override
        public void on(final TodoId id, final AggregateVersion aggregateVersion, final NewTodoCreated event) {
            Objects.requireNonNull(id);
            Objects.requireNonNull(aggregateVersion);
            Objects.requireNonNull(event);
            // On a real sample it should call a ComandHandler
            events.add(new On(id, aggregateVersion, event));
        }

        @Override
        public Class<NewTodoCreated> eventType() {
            return NewTodoCreated.class;
        }
    }

    @Test
    void shouldListenToEvent() throws CommandException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("lorem ipsum", Owner.USER_1);

        // When
        commandHandler.handle(creational, givenCreateTodo, CommandHandlerTest.DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(onStoredEventListeners.size()).isEqualTo(1),
                () -> assertThat(onNewTodoCreated.events.size()).isEqualTo(1),
                () -> assertThat(onNewTodoCreated.events.getFirst().id()).isEqualTo(TodoId.USER_1_TODO_1),
                () -> assertThat(onNewTodoCreated.events.getFirst().aggregateVersion()).isEqualTo(new AggregateVersion(0)),
                () -> assertThat(onNewTodoCreated.events.getFirst().event()).isEqualTo(new NewTodoCreated("lorem ipsum", Owner.USER_1))
        );
    }
}
