package com.damdamdeo.pulse.extension.core.executedby;

import java.util.Objects;
import java.util.Optional;

public sealed interface ExecutedBy
        permits ExecutedBy.Anonymous, ExecutedBy.EndUser, ExecutedBy.ServiceAccount, ExecutedBy.NotAvailable {

    String SEPARATOR = ":";

    String encode(ExecutedByEncoder executedByEncoder);

    String value();

    boolean decoded();

    static ExecutedBy decode(final String value, final ExecutedByDecoder executedByDecoder) {
        if (value == null || value.equals("NA")) {
            return NotAvailable.INSTANCE;
        } else if (value.equals("A")) {
            return Anonymous.INSTANCE;
        } else if (value.startsWith(EndUser.DISCRIMINANT + SEPARATOR)) {
            final String encodedEndUser = value.substring((EndUser.DISCRIMINANT + SEPARATOR).length());
            final Optional<String> decoded = executedByDecoder.decode(encodedEndUser);
            return decoded.map(decodedEndUser -> new EndUser(decodedEndUser, true))
                    .orElseGet(() -> new EndUser(encodedEndUser, false));
        } else if (value.startsWith(ServiceAccount.DISCRIMINANT + SEPARATOR)) {
            return new ServiceAccount(value.substring((ServiceAccount.DISCRIMINANT + SEPARATOR).length()));
        }
        throw new IllegalArgumentException("Invalid ExecutedBy value: " + value);
    }

    final class Anonymous implements ExecutedBy {
        public static final String DISCRIMINANT = "A";

        public static final Anonymous INSTANCE = new Anonymous();

        private Anonymous() {
        }

        @Override
        public String encode(final ExecutedByEncoder executedByEncoder) {
            return DISCRIMINANT;
        }

        @Override
        public String value() {
            return DISCRIMINANT;
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
        public String encode(final ExecutedByEncoder executedByEncoder) {
            if (!decoded) {
                throw new IllegalStateException("Could not encode not decoded");
            }
            return DISCRIMINANT + SEPARATOR + new String(executedByEncoder.encode(by));
        }

        @Override
        public String value() {
            return DISCRIMINANT + SEPARATOR + by;
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
        public String encode(final ExecutedByEncoder executedByEncoder) {
            return DISCRIMINANT + SEPARATOR + by;
        }

        @Override
        public String value() {
            return DISCRIMINANT + SEPARATOR + by;
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
        public String encode(final ExecutedByEncoder executedByEncoder) {
            return DISCRIMINANT;
        }

        @Override
        public String value() {
            return DISCRIMINANT;
        }

        @Override
        public boolean decoded() {
            return true;
        }
    }
}