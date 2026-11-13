package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.spi.DeploymentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ShouldFailWhenProjectionValueObjectContainsUnsupportedType {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(archive -> archive.addClasses(TestProjection.class, ValueObject.class, Unsupported.class))
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withConfigurationResource("application.properties")
            .assertException(throwable -> {
                final DeploymentException exception = Assertions.assertInstanceOf(
                        DeploymentException.class, throwable);
                Assertions.assertAll(
                        () -> assertTrue(exception.getMessage().contains("TestProjection.value.child'")),
                        () -> assertTrue(exception.getMessage().contains("TestProjection.values.child'")),
                        () -> assertTrue(exception.getMessage().contains("TestProjection.array[].child'")));
            });

    public record TestProjection(ProjectionId<TodoId> id,
                                 ValueObject value,
                                 List<ValueObject> values,
                                 ValueObject[] array) implements Projection<TodoId> {
    }

    public record ValueObject(Unsupported child) {
    }

    public static final class Unsupported {
    }

    @Test
    void shouldRejectUnsupportedTypeInsideRecordValueObject() {
        fail("Startup should have failed");
    }
}
