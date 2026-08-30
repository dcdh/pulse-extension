package com.damdamdeo.pulse.extension.common.runtime.executedby;

import com.damdamdeo.pulse.extension.common.runtime.StubPassphraseRepository;
import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.ExecutedByHashed;
import com.damdamdeo.pulse.extension.core.executedby.UsernameHasher;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class UsernameHasherTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(javaArchive -> javaArchive.addClass(StubPassphraseRepository.class))
            .withConfigurationResource("application.properties");

    @Inject
    UsernameHasher usernameHasher;

    @Test
    void shouldReturnUnknown() {
        // Given
        final ExecutedBy executedBy = new ExecutedBy.EndUser(new Username("alice@mail.com"));

        // When
        final ExecutedByHashed hashed = executedBy.hash(usernameHasher);

        // Then
        assertThat(hashed).isEqualTo(new ExecutedByHashed("EU:4714636ab5e7b6ec200c9a0ec8a1b08f61df989c47f22f9e9322adf63922d9e4"));
    }
}
