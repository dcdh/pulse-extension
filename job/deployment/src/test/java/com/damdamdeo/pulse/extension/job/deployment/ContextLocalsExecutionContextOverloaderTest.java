package com.damdamdeo.pulse.extension.job.deployment;

import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.job.runtime.executedby.ContextLocalsExecutionContextOverloader;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;

class ContextLocalsExecutionContextOverloaderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(InMemoryTodoRepository.class))
            .withConfigurationResource("application.properties");

    @Inject
    ContextLocalsExecutionContextOverloader contextLocalsExecutionContextOverloader;

    @Test
    @RunOnVertxContext
    void shouldOverloadAddAJobNameToContextLocals() {
        // Given
        contextLocalsExecutionContextOverloader.overload(new JobName("SampleJob"));

        // When
        Optional<JobName> executedBy = contextLocalsExecutionContextOverloader.current();

        // Then
        assertThat(executedBy).isEqualTo(Optional.of(new JobName("SampleJob")));
    }

    @Test
    @RunOnVertxContext
    void shouldCleanRemoveJobNameToContextLocals() {
        // Given
        contextLocalsExecutionContextOverloader.overload(new JobName("SampleJob"));

        // When
        contextLocalsExecutionContextOverloader.clean(new JobName("SampleJob"));

        // Then
        assertThat(contextLocalsExecutionContextOverloader.current()).isEqualTo(Optional.empty());
    }

    @Test
    void isolatesBlockingWorkerThreadsAndCleansTheirContext() throws Exception {
        final JobName own = new JobName("BlockingJob");
        contextLocalsExecutionContextOverloader.overload(own);
        try {
            assertThat(contextLocalsExecutionContextOverloader.current()).contains(own);
            try (final var executor = newSingleThreadExecutor()) {
                assertThat(executor.submit(() -> {
                    assertThat(contextLocalsExecutionContextOverloader.current()).isEmpty();
                    final JobName other = new JobName("OtherJob");
                    contextLocalsExecutionContextOverloader.overload(other);
                    try {
                        return contextLocalsExecutionContextOverloader.current();
                    } finally {
                        contextLocalsExecutionContextOverloader.clean(other);
                    }
                }).get()).contains(new JobName("OtherJob"));
                assertThat(executor.submit(() -> contextLocalsExecutionContextOverloader.current()).get()).isEmpty();
            }
            assertThat(contextLocalsExecutionContextOverloader.current()).contains(own);
        } finally {
            contextLocalsExecutionContextOverloader.clean(own);
        }
        assertThat(contextLocalsExecutionContextOverloader.current()).isEmpty();
    }

}
