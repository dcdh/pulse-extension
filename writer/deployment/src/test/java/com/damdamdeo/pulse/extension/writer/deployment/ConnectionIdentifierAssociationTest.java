package com.damdamdeo.pulse.extension.writer.deployment;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierAssociation;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionIdentifierAssociationTest extends AbstractWriterTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-oidc", Version.getVersion())
            ));

    @Inject
    Instance<ConnectionIdentifierAssociation> connectionIdentifierAssociationInstance;

    @Test
    void shouldRegisterConnectionIdentifierBean() {
        assertThat(connectionIdentifierAssociationInstance.isResolvable()).isTrue();
    }
}
