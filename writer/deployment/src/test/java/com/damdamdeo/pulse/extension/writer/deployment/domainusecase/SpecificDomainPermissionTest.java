package com.damdamdeo.pulse.extension.writer.deployment.domainusecase;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.command.MarkTodoAsDone;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.usecase.UseCaseException;
import com.damdamdeo.pulse.extension.core.usecase.permission.SpecificDomain;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificDomainPermissionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    static class SampleSpecificDomain extends SpecificDomain<TodoId, MarkTodoAsDone> {

        @Override
        public boolean allow(final TodoId aggregateId, final MarkTodoAsDone command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
            return false;
        }

        @Override
        public boolean allow(final MarkTodoAsDone command, final PermissionExecutionContext permissionExecutionContext) throws UseCaseException {
            return false;
        }
    }

    @Inject
    Instance<SampleSpecificDomain> instance;

    @Test
    void shouldBeResolvable() {
        assertThat(instance.isResolvable()).isTrue();
    }
}
