package com.damdamdeo.pulse.extension.encryption.storage.deployment;

import com.damdamdeo.pulse.extension.core.encryption.*;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class StubPassphraseRepository implements PassphraseRepository {

    final List<String> called = new ArrayList<>();
    final Map<OwnedBy, Passphrase> stored = new HashMap<>();

    @Override
    public Optional<Passphrase> findBy(final OwnedBy ownedBy) {
        called.add("retrieve" + ownedBy.id());
        return stored.containsKey(ownedBy) ? Optional.of(stored.get(ownedBy)) : Optional.empty();
    }

    @Override
    public Passphrase get(final OwnedBy ownedBy) throws UnableToRetrievePassphraseException, UnknownPassphraseException {
        called.add("get" + ownedBy.id());
        if (stored.containsKey(ownedBy)) {
            return stored.get(ownedBy);
        }
        throw new UnknownPassphraseException(ownedBy);
    }

    @Override
    public List<RetrievedPassphrase> list(final List<OwnedBy> multiples) throws UnableToRetrievePassphraseException {
        called.add("retrieve" + multiples.stream().map(OwnedBy::id).collect(Collectors.joining(",")));
        return multiples.stream().map(ownedBy -> new RetrievedPassphrase(ownedBy, stored.get(ownedBy))).toList();
    }

    @Override
    public Passphrase store(final OwnedBy ownedBy, final Passphrase passphrase) throws PassphraseAlreadyExistsException {
        called.add("store" + ownedBy.id() + new String(passphrase.passphrase()));
        stored.put(ownedBy, passphrase);
        return passphrase;
    }

    @Override
    public Passphrase update(final OwnedBy ownedBy, final Passphrase passphrase) throws UnableToStorePassphraseException, UnknownPassphraseException {
        called.add("update" + ownedBy.id() + (passphrase.passphrase() != null ? new String(passphrase.passphrase()) : ""));
        if (!stored.containsKey(ownedBy)) {
            throw new UnknownPassphraseException(ownedBy);
        }
        stored.put(ownedBy, passphrase);
        return passphrase;
    }

    public void reset() {
        this.called.clear();
        this.stored.clear();
    }

    public List<String> getCalled() {
        return called;
    }
}
