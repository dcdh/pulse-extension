package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public sealed interface ExecutedBy
        permits ExecutedBy.Anonymous, ExecutedBy.EndUser, ExecutedBy.ServiceAccount, ExecutedBy.NotAvailable {

    String SEPARATOR = ":";

    ExecutedByEncoded encode(UsernameEncoder usernameEncoder, OwnedBy ownedBy) throws UnableToEncodeException;

    ExecutedByHashed hash(UsernameHasher usernameHasher);

    String value();

    Username username();

    final class Anonymous implements ExecutedBy {

        public static final String DISCRIMINANT = "A";

        public static final Anonymous INSTANCE = new Anonymous();

        private Anonymous() {
        }

        @Override
        public ExecutedByEncoded encode(final UsernameEncoder usernameEncoder, final OwnedBy ownedBy) {
            return new ExecutedByEncoded(DISCRIMINANT);
        }

        @Override
        public ExecutedByHashed hash(final UsernameHasher usernameHasher) {
            return new ExecutedByHashed(DISCRIMINANT);
        }

        @Override
        public String value() {
            return DISCRIMINANT;
        }

        @Override
        public Username username() {
            throw new UnsupportedOperationException("Anonymous does not have a username");
        }
    }

    record EndUser(Username username) implements ExecutedBy {
        public static final String DISCRIMINANT = "EU";

        public EndUser {
            Objects.requireNonNull(username);
        }

        @Override
        public ExecutedByEncoded encode(final UsernameEncoder usernameEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            return new ExecutedByEncoded(DISCRIMINANT + SEPARATOR + usernameEncoder.encode(username(), ownedBy).encoded());
        }

        @Override
        public ExecutedByHashed hash(final UsernameHasher usernameHasher) {
            return new ExecutedByHashed(DISCRIMINANT + SEPARATOR + usernameHasher.hash(username()).hashed());
        }

        @Override
        public String value() {
            return DISCRIMINANT + SEPARATOR + username.username();
        }
    }

    record ServiceAccount(String by) implements ExecutedBy {

        public static final String DISCRIMINANT = "SA";

        public ServiceAccount {
            Objects.requireNonNull(by);
            if (by.isBlank()) {
                throw new IllegalArgumentException("by must not be blank");
            }
        }

        @Override
        public ExecutedByEncoded encode(final UsernameEncoder usernameEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            return new ExecutedByEncoded(DISCRIMINANT + SEPARATOR + by);
        }

        @Override
        public ExecutedByHashed hash(final UsernameHasher usernameHasher) {
            return new ExecutedByHashed(DISCRIMINANT + SEPARATOR + by);
        }

        @Override
        public String value() {
            return DISCRIMINANT + SEPARATOR + by;
        }

        @Override
        public Username username() {
            throw new UnsupportedOperationException("Service account does not have a username");
        }
    }

    final class NotAvailable implements ExecutedBy {

        public static final String DISCRIMINANT = "NA";

        public static final NotAvailable INSTANCE = new NotAvailable();

        private NotAvailable() {
        }

        @Override
        public ExecutedByEncoded encode(final UsernameEncoder usernameEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            return new ExecutedByEncoded(DISCRIMINANT);
        }

        @Override
        public ExecutedByHashed hash(final UsernameHasher usernameHasher) {
            return new ExecutedByHashed(DISCRIMINANT);
        }

        @Override
        public String value() {
            return DISCRIMINANT;
        }

        @Override
        public Username username() {
            throw new UnsupportedOperationException("Not available does not have a username");
        }
    }
}
