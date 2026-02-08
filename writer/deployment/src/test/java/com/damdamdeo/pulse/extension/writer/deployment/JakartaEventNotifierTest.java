package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.CommandHandler;
import com.damdamdeo.pulse.extension.core.command.CreateTodo;
import com.damdamdeo.pulse.extension.core.event.IdentifiableEvent;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JakartaEventNotifierTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.compose.devservices.enabled", "true")
            .withConfigurationResource("application.properties");

    @Inject
    CommandHandler<Todo, TodoId> commandHandler;

    @Inject
    EventListener eventListener;

    @Singleton
    static class EventListener {

        final List<IdentifiableEvent> identifiableEvents = new ArrayList<>();
        NewTodoCreated newTodoCreated = null;

        void on(@Observes final IdentifiableEvent identifiableEvent) {
            identifiableEvents.add(identifiableEvent);
            identifiableEvent.executeOn(NewTodoCreated.class, (event) -> newTodoCreated = event);
        }
    }

    @Test
    void shouldListenToEvent() {
        // Given
        final CreateTodo givenCreateTodo = new CreateTodo(new TodoId("Damien", 20L), "lorem ipsum");

        // When
        commandHandler.handle(givenCreateTodo);

        // Then
        assertAll(
                () -> assertThat(eventListener.identifiableEvents.size()).isEqualTo(1),
                () -> assertThat(eventListener.identifiableEvents.getFirst().id()).isEqualTo("Damien/20"),
                () -> assertThat(eventListener.identifiableEvents.getFirst().event()).isEqualTo(new NewTodoCreated("lorem ipsum")),
                () -> assertThat(eventListener.newTodoCreated).isEqualTo(new NewTodoCreated("lorem ipsum"))
        );
    }
}
