package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public sealed interface Audience
    permits Audience.AllConnected, Audience.FromListOfEligibility {

    String SEPARATOR = ":";

    String ELIGIBLE_SEPARATOR = ",";

    boolean eligible(ExecutedBy.EndUser executedBy);

    String encode();

    static Audience decode(final String value) {
        Objects.requireNonNull(value);
        if (value.startsWith(AllConnected.DISCRIMINANT)) {
            return AllConnected.INSTANCE;
        } else if (value.startsWith(FromListOfEligibility.DISCRIMINANT)) {
            final List<ExecutedBy.EndUser> listOfEligibilityEndUser = Arrays.stream(value.substring((FromListOfEligibility.DISCRIMINANT+SEPARATOR).length())
                    .split(ELIGIBLE_SEPARATOR))
                    .map(eligible -> {
                        Validate.validState(eligible.startsWith(ExecutedBy.EndUser.DISCRIMINANT + ExecutedBy.EndUser.SEPARATOR));
                        return (ExecutedBy.EndUser) ExecutedBy.decode(eligible);
                    })
                    .toList();
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
        public String encode() {
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
        public String encode() {
            return DISCRIMINANT + SEPARATOR + eligibles.stream()
                    .map(ExecutedBy.EndUser::encode)
                    .collect(Collectors.joining(ELIGIBLE_SEPARATOR));
        }
    }
}
