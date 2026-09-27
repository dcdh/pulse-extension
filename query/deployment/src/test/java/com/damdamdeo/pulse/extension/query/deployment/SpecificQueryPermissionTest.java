package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.permission.PermissionExecutionContext;
import com.damdamdeo.pulse.extension.core.query.QueryException;
import com.damdamdeo.pulse.extension.core.query.permission.SpecificQuery;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificQueryPermissionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties");

    static class SampleSpecificQuery extends SpecificQuery<TodoId> {

        @Override
        public boolean allow(final Set<TodoId> aggregateIds,
                             final PermissionExecutionContext permissionExecutionContext) throws QueryException {
            return false;
        }
    }

    @Inject
    Instance<SampleSpecificQuery> instance;

    @Test
    void shouldBeResolvable() {
        assertThat(instance.isResolvable()).isTrue();
    }
}
