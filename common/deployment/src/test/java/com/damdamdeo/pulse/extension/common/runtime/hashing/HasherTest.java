package com.damdamdeo.pulse.extension.common.runtime.hashing;

import com.damdamdeo.pulse.extension.core.hashing.Algorithm;
import com.damdamdeo.pulse.extension.core.hashing.Hash;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class HasherTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.devservices.enabled", "false");

    @Inject
    HasherExecutor hasherExecutor;

    @Test
    void shouldHashUsingSHA3_256Test() {
        // Given
        final byte[] givenOriginal = "test".getBytes();

        // When
        final Hash hash = hasherExecutor.hash(Algorithm.SHA3_256, givenOriginal);

        // Then
        assertThat(hash).isEqualTo(new Hash(Algorithm.SHA3_256, "36f028580bb02cc8272a9a020f4200e346e276ae664e45ee80745574e2f5ab80"));
    }
}
