package com.damdamdeo.pulse.extension.core.executedby;

import java.util.Objects;

public sealed interface ExecutedBy
        permits ExecutedBy.Anonymous, ExecutedBy.EndUser, ExecutedBy.ServiceAccount, ExecutedBy.NotAvailable {

    String SEPARATOR = ":";

    String encode();

    static ExecutedBy decode(final String value) {
        if (value == null || value.equals("NA")) {
            return NotAvailable.INSTANCE;
        } else if (value.equals("A")) {
            return Anonymous.INSTANCE;
        } else if (value.startsWith(EndUser.DISCRIMINANT + SEPARATOR)) {
            return new EndUser(value.substring((EndUser.DISCRIMINANT + SEPARATOR).length()));
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
        public String encode() {
            return DISCRIMINANT;
        }
    }

    record EndUser(String by) implements ExecutedBy {
        public static final String DISCRIMINANT = "EU";

        public EndUser {
            Objects.requireNonNull(by);
            if (by.isBlank()) {
                throw new IllegalArgumentException("by must not be blank");
            }
        }

        @Override
        public String encode() {
            return DISCRIMINANT + SEPARATOR + by;
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
        public String encode() {
            return DISCRIMINANT + SEPARATOR + by;
        }
    }

    final class NotAvailable implements ExecutedBy {
        public static final String DISCRIMINANT = "NA";

        public static final NotAvailable INSTANCE = new NotAvailable();

        private NotAvailable() {
        }

        @Override
        public String encode() {
            return DISCRIMINANT;
        }
    }
}