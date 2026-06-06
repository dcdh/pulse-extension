package com.damdamdeo.pulse.extension.common.runtime.connectionidentifier;

import com.damdamdeo.pulse.extension.common.runtime.AbstractCommonTest;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionAssociationFinder;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionAssociationFinderTest extends AbstractCommonTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.oidc.client-id", "account")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion())));

    @Inject
    Instance<ConnectionAssociationFinder> connectionAssociationFinderInstance;

    @Test
    void shouldRegisterConnectionIdentifierBean() {
        assertThat(connectionAssociationFinderInstance.isResolvable()).isTrue();
    }
}
