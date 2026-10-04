package com.damdamdeo.pulse.extension.job.deployment;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.job.JobName;
import com.damdamdeo.pulse.extension.job.runtime.executedby.ContextLocalsExecutionContextOverloader;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ContextLocalsExecutionContextOverloaderTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
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

}
