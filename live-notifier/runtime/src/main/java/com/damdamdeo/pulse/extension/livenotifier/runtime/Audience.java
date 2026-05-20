package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.executedby.*;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public sealed interface Audience
        permits Audience.AllConnected, Audience.FromListOfEligibility {

    String SEPARATOR = ":";

    String ELIGIBLE_SEPARATOR = ",";

    boolean eligible(ExecutedBy.EndUser executedBy);

    String encode(ExecutedByEncoder executedByEncoder, OwnedBy ownedBy) throws UnableToEncodeException;

    static Audience decode(final String value, final ExecutedByFactory executedByFactory, final OwnedBy ownedBy) throws UnableToDecodeException {
        Objects.requireNonNull(value);
        Objects.requireNonNull(executedByFactory);
        Objects.requireNonNull(ownedBy);
        if (value.startsWith(AllConnected.DISCRIMINANT)) {
            return AllConnected.INSTANCE;
        } else if (value.startsWith(FromListOfEligibility.DISCRIMINANT)) {
            final List<ExecutedBy.EndUser> listOfEligibilityEndUser = new ArrayList<>();
            for (final String executedBy : value.substring((FromListOfEligibility.DISCRIMINANT + SEPARATOR).length())
                    .split(ELIGIBLE_SEPARATOR)) {
                Validate.validState(executedBy.startsWith(ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.EndUser.SEPARATOR));
                final ExecutedBy.EndUser apply = (ExecutedBy.EndUser) executedByFactory.from(executedBy, ownedBy);
                if (apply.decoded()) {
                    listOfEligibilityEndUser.add(apply);
                }
            }
            return new FromListOfEligibility(listOfEligibilityEndUser);
        }
        throw new IllegalArgumentException("Invalid Audience value: " + value);
    }

    final class AllConnected implements Audience {
        public static final String DISCRIMINANT = "ALL_CONNECTED";

        public static final AllConnected INSTANCE = new AllConnected();

        private AllConnected() {
        }

        @Override
        public boolean eligible(final ExecutedBy.EndUser executedBy) {
            return true;
        }

        @Override
        public String encode(final ExecutedByEncoder executedByEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            return DISCRIMINANT;
        }
    }

    record FromListOfEligibility(List<ExecutedBy.EndUser> eligibles) implements Audience {
        public static final String DISCRIMINANT = "FROM_LIST_OF_ELIGIBILITY";

        public FromListOfEligibility {
            Objects.requireNonNull(eligibles);
        }

        @Override
        public boolean eligible(final ExecutedBy.EndUser executedBy) {
            return eligibles.contains(executedBy);
        }

        @Override
        public String encode(final ExecutedByEncoder executedByEncoder, final OwnedBy ownedBy) throws UnableToEncodeException {
            final StringJoiner joiner = new StringJoiner(ELIGIBLE_SEPARATOR);
            for (final ExecutedBy.EndUser endUser : eligibles) {
                final String encode = endUser.encode(executedByEncoder, ownedBy);
                joiner.add(encode);
            }
            return DISCRIMINANT + SEPARATOR + joiner;
        }
    }
}
