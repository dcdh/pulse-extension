package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.AggregateVersion;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.NewTodoCreated;
import com.damdamdeo.pulse.extension.core.listener.AbstractOnStoredEventListener;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertSame;

class OnStoredEventListenerScopeTest extends AbstractWriterTest {

    @RegisterExtension
    static final QuarkusUnitTest application = new QuarkusUnitTest()
            .withApplicationRoot(archive -> archive.addClasses(Owner.class, Listener.class))
            .withConfigurationResource("application.properties");

    @Inject
    Owner owner;

    @Singleton
    public static class Owner {
        final Listener listener;

        @Inject
        public Owner(final Listener listener) {
            this.listener = listener;
        }
    }

    public static class Listener extends AbstractOnStoredEventListener<TodoId, NewTodoCreated> {
        private final Owner owner;

        @Inject
        public Listener(final Owner owner) {
            this.owner = owner;
        }

        public Owner owner() {
            return owner;
        }

        @Override
        public void on(final TodoId id, final AggregateVersion version, final NewTodoCreated event) {
        }

        @Override
        public Class<NewTodoCreated> eventType() {
            return NewTodoCreated.class;
        }
    }

    @Test
    void resolvesListenerCyclesThroughNormalScopeProxy() {
        assertSame(owner, owner.listener.owner());
    }
}
