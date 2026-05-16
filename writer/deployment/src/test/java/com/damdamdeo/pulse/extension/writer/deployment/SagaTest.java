package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.*;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.saga.Saga;
import io.quarkus.arc.All;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SagaTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(CommandHandlerTest.DuplicateTodoException.class))
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "true")
            .overrideRuntimeConfigKey("pulse.datasource.init-at-startup", "true")
            .withConfigurationResource("application.properties");

    @Inject
    CommandHandler<Todo, TodoId> commandHandler;

    @Inject
    @All
//    List<Saga<TodoId, Event<TodoId>>> sagas; not working ! will return empty
    List<Saga<TodoId, ?>> sagas;// It's ok

    Function<SequenceNumber, TodoId> creational = sequenceNumber -> new TodoId("Damien", sequenceNumber);

    @Inject
    OnNewTodoCreated onNewTodoCreated;

    record On(TodoId id, NewTodoCreated event) {
    }

    static class OnNewTodoCreated implements Saga<TodoId, NewTodoCreated> {

        final List<On> events = new ArrayList<>();

        @Override
        public void on(final TodoId id, final NewTodoCreated event) {
            Objects.requireNonNull(id);
            Objects.requireNonNull(event);
            // On a real sample it should call a ComandHandler
            events.add(new On(id, event));
        }

        @Override
        public Class<NewTodoCreated> eventType() {
            return NewTodoCreated.class;
        }
    }

    @Test
    void shouldListenToEvent() throws BusinessException, SequenceGenerationException {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo("lorem ipsum");

        // When
        commandHandler.handle(creational, givenCreateTodo, CommandHandlerTest.DuplicateTodoException::new);

        // Then
        assertAll(
                () -> assertThat(sagas.size()).isEqualTo(1),
                () -> assertThat(onNewTodoCreated.events.size()).isEqualTo(1),
                () -> assertThat(onNewTodoCreated.events.getFirst().id()).isEqualTo(new TodoId("Damien", TodoId.SEQUENCE_NUMBER_1)),
                () -> assertThat(onNewTodoCreated.events.getFirst().event()).isEqualTo(new NewTodoCreated("lorem ipsum"))
        );
    }
}
