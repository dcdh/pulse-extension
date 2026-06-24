package com.damdamdeo.pulse.extension.core.executedby;

import com.damdamdeo.pulse.extension.core.connecteduser.Username;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;

import java.util.Objects;

public sealed interface ExecutedBy
        permits ExecutedBy.Anonymous, ExecutedBy.EndUser, ExecutedBy.ServiceAccount, ExecutedBy.NotAvailable {

    String SEPARATOR = ":";

    String encode(ExecutedByEncoder executedByEncoder, OwnedBy ownedBy) throws UnableToEncodeException;

    String value();

    Username username();

    boolean decoded();

    final class Anonymous implements ExecutedBy {

        public static final String DISCRIMINANT = "A";

        public static final Anonymous INSTANCE = new Anonymous();

        private Anonymous() {
        }

        @Override
        public String encode(final ExecutedByEncoder executedByEncoder, final OwnedBy ownedBy) {
            return DISCRIMINANT;
        }

        @Override
        public String value() {
            return DISCRIMINANT;
        }

        @Override
        public Username username() {
            throw new UnsupportedOperationException("Anonymous does not have a username");
        }

        @Override
        public boolean decoded() {
            return true;
        }
    }

    record EndUser(String by, boolean decoded) implements ExecutedBy {
        public static final String DISCRIMINANT = "EU";

        public EndUser {
            Objects.requireNonNull(by);
            if (by.isBlank()) {
                throw new IllegalArgumentException("by must not be blank");
            }
        }

        @Override
        public String encode(final ExecutedByEncoder executedByEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            if (!decoded) {
                throw new IllegalStateException("Could not encode not decoded");
            }
            return DISCRIMINANT + SEPARATOR + new String(executedByEncoder.encode(by, ownedBy));
        }

        @Override
        public String value() {
            return DISCRIMINANT + SEPARATOR + by;
        }

        @Override
        public Username username() {
            return new Username(by);
        }

        @Override
        public boolean decoded() {
            return decoded;
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
        public String encode(final ExecutedByEncoder executedByEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            return DISCRIMINANT + SEPARATOR + by;
        }

        @Override
        public String value() {
            return DISCRIMINANT + SEPARATOR + by;
        }

        @Override
        public Username username() {
            throw new UnsupportedOperationException("Service account does not have a username");
        }

        @Override
        public boolean decoded() {
            return true;
        }
    }

    final class NotAvailable implements ExecutedBy {

        public static final String DISCRIMINANT = "NA";

        public static final NotAvailable INSTANCE = new NotAvailable();

        private NotAvailable() {
        }

        @Override
        public String encode(final ExecutedByEncoder executedByEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            return DISCRIMINANT;
        }

        @Override
        public String value() {
            return DISCRIMINANT;
        }

        @Override
        public Username username() {
            throw new UnsupportedOperationException("Not available does not have a username");
        }

        @Override
        public boolean decoded() {
            return true;
        }
    }
}
