package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.executedby.ExecutedBy;
import com.damdamdeo.pulse.extension.core.executedby.TestExecutedByDecoder;
import com.damdamdeo.pulse.extension.core.executedby.TestExecutedByEncoder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class AudienceTest {

    private static final ExecutedBy.EndUser ALICE = new ExecutedBy.EndUser("alice", true);
    private static final ExecutedBy.EndUser BOB = new ExecutedBy.EndUser("bob", true);

    // ----------------------------------------------------------------------
    // AllConnected
    // ----------------------------------------------------------------------

    @Test
    void allConnected_should_be_eligible_for_alice_and_bob() {
        // Given
        final Audience audience = Audience.AllConnected.INSTANCE;

        // Then
        assertAll(
                () -> assertThat(audience.eligible(ALICE)).isTrue(),
                () -> assertThat(audience.eligible(BOB)).isTrue());
    }

    @Test
    void allConnected_encode_and_decode_should_return_singleton_instance() {
        // Given
        final Audience audience = Audience.AllConnected.INSTANCE;

        // When
        final String encoded = audience.encode(TestExecutedByEncoder.INSTANCE);
        final Audience decoded = Audience.decode(encoded, TestExecutedByDecoder.INSTANCE);

        // Then
        assertAll(
                () -> assertThat(encoded).isEqualTo("ALL_CONNECTED"),
                () -> assertThat(decoded).isSameAs(Audience.AllConnected.INSTANCE));
    }

    // ----------------------------------------------------------------------
    // FromListOfEligibility
    // ----------------------------------------------------------------------

    @Test
    void fromList_should_allow_only_alice_when_only_alice_is_eligible() {
        // Given
        final Audience audience = new Audience.FromListOfEligibility(List.of(ALICE));

        // Then
        assertAll(
                () -> assertThat(audience.eligible(ALICE)).isTrue(),
                () -> assertThat(audience.eligible(BOB)).isFalse());
    }

    @Test
    void fromList_should_allow_alice_and_bob_when_both_are_eligible() {
        // Given
        final Audience audience = new Audience.FromListOfEligibility(List.of(ALICE, BOB));

        // Then
        assertAll(
                () -> assertThat(audience.eligible(ALICE)).isTrue(),
                () -> assertThat(audience.eligible(BOB)).isTrue());
    }

    @Test
    void fromList_encode_and_decode_should_preserve_eligibility_list() {
        // Given
        final Audience audience = new Audience.FromListOfEligibility(List.of(ALICE, BOB));

        // When
        final String encoded = audience.encode(TestExecutedByEncoder.INSTANCE);
        final Audience decoded = Audience.decode(encoded, TestExecutedByDecoder.INSTANCE);

        // Then
        assertThat(decoded).isInstanceOf(Audience.FromListOfEligibility.class);

        final Audience.FromListOfEligibility decodedTyped = (Audience.FromListOfEligibility) decoded;

        assertAll(
                () -> assertThat(encoded).isEqualTo("FROM_LIST_OF_ELIGIBILITY:EU:encodedalice,EU:encodedbob"),
                () -> assertThat(decodedTyped.eligibles()).containsExactly(ALICE, BOB),
                () -> assertThat(decodedTyped.eligible(ALICE)).isTrue(),
                () -> assertThat(decodedTyped.eligible(BOB)).isTrue());
    }

    // ----------------------------------------------------------------------
    // Decode errors
    // ----------------------------------------------------------------------

    @Test
    void decode_should_fail_on_unknown_discriminant() {
        assertThatThrownBy(() -> Audience.decode("UNKNOWN", TestExecutedByDecoder.INSTANCE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Audience value");
    }

    @Test
    void decode_should_fail_on_invalid_eligible_format() {
        // Given
        String invalid = Audience.FromListOfEligibility.DISCRIMINANT + Audience.SEPARATOR + "INVALID_FORMAT";

        // Then
        assertThatThrownBy(() -> Audience.decode(invalid, TestExecutedByDecoder.INSTANCE))
                .isInstanceOf(IllegalStateException.class);
    }
}