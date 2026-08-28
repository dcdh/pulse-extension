package com.damdamdeo.pulse.extension.query.deployment.file;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.file.CustomMetadata;
import com.damdamdeo.pulse.extension.core.query.file.EncryptedCustomMetadata;
import com.damdamdeo.pulse.extension.core.query.file.MetadataEncryptionException;
import com.damdamdeo.pulse.extension.query.deployment.StubPassphraseProvider;
import com.damdamdeo.pulse.extension.query.runtime.file.DefaultCustomMetadataEncryption;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCustomMetadataEncryptionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(StubPassphraseProvider.class))
            .withConfigurationResource("application.properties");

    @Inject
    DefaultCustomMetadataEncryption defaultCustomMetadataEncryption;

    @Test
    void shouldEncryptAndDecrypt() throws MetadataEncryptionException {
        // Given
        final CustomMetadata givenCustomMetadata = new CustomMetadata(Map.of("key", "value"));

        // When
        final EncryptedCustomMetadata encrypted = defaultCustomMetadataEncryption.encrypt(givenCustomMetadata, OwnedBy.from(TodoId.USER_1_TODO_1));
        final CustomMetadata decrypted = defaultCustomMetadataEncryption.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(givenCustomMetadata);
    }
}
