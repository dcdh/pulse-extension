package com.damdamdeo.pulse.extension.consumer.deployment;

import com.damdamdeo.pulse.extension.core.PassphraseSample;
import com.damdamdeo.pulse.extension.core.Todo;
import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;
import java.util.Optional;

public abstract class AbstractConsumerTest {

    @ApplicationScoped
    @Priority(1)
    @Alternative
    static class StubPassphraseRepository implements PassphraseRepository {

        @Override
        public Optional<Passphrase> findBy(final OwnedBy ownedBy) {
            if (Todo.OWNED_BY_USER_1.equals(ownedBy)) {
                return Optional.of(PassphraseSample.PASSPHRASE_1);
            } else if (Todo.OWNED_BY_USER_2.equals(ownedBy)) {
                return Optional.of(new Passphrase(null));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Passphrase get(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException, UnknownPassphraseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public List<RetrievedPassphrase> list(List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
            throw new IllegalStateException("Should not be called !");
        }

        @Override
        public Passphrase update(final OwnedBy ownedBy, final Passphrase passphrase) throws UnableToStorePassphraseException, UnknownPassphraseException {
            throw new IllegalStateException("Should not be called");
        }
    }
}
