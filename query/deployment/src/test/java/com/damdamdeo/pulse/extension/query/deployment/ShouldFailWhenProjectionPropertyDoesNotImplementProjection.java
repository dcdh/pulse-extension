package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.Status;
import com.damdamdeo.pulse.extension.core.TodoChecklistId;
import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShouldFailWhenProjectionPropertyDoesNotImplementProjection {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .assertException(throwable -> assertThat(throwable)
                    .isExactlyInstanceOf(DeploymentException.class)
                    .hasMessage("Found 2 deployment problems: \n" +
                            "[1] Invalid projection property 'com.damdamdeo.pulse.extension.query.deployment.ShouldFailWhenProjectionPropertyDoesNotImplementProjection$TodoProjection.checklist'. Type 'com.damdamdeo.pulse.extension.query.deployment.ShouldFailWhenProjectionPropertyDoesNotImplementProjection$TodoChecklistProjection' must implement Projection or AggregateId.\n" +
                            "[2] Invalid projection property 'com.damdamdeo.pulse.extension.query.deployment.ShouldFailWhenProjectionPropertyDoesNotImplementProjection$TodoProjection.checklists'. Type 'com.damdamdeo.pulse.extension.query.deployment.ShouldFailWhenProjectionPropertyDoesNotImplementProjection$TodoChecklistProjection' must implement Projection or AggregateId."));

    public record TodoProjection(TodoId todoId,
                                 String description,
                                 Status status,
                                 Boolean important,
                                 TodoChecklistProjection checklist,
                                 List<TodoChecklistProjection> checklists) implements Projection {
    }

    public record TodoChecklistProjection(TodoChecklistId todoChecklistId, String description) {
    }

    @Test
    void test() {
        Assertions.fail("Startup should have failed");
    }
}
