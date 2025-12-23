package com.damdamdeo.pulse.extension.livenotifier.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.encryption.Passphrase;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseAlreadyExistsException;
import com.damdamdeo.pulse.extension.core.encryption.PassphraseRepository;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.livenotifier.TopicManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

public abstract class AbstractMessagingTest {

    @BeforeEach
    @AfterEach
    void purgeTopics() {
        TopicManager.resetTopics();
    }

    @ApplicationScoped
    static public class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> retrieve(final OwnedBy ownedBy) {
            return Optional.of(PassphraseSample.PASSPHRASE);
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new UnsupportedOperationException("Should not be called");
        }
    }
}
