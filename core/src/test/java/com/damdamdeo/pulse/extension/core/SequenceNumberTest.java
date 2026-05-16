package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SequenceNumberTest {

    @ParameterizedTest
    @MethodSource("provideSequenceNumber")
    void shouldComputeSequenceNumber(final Long givenNumber, final String expectedSequenceNumber) {
        assertThat(SequenceNumber.fromNumber(givenNumber).number()).isEqualTo(expectedSequenceNumber);
    }

    private static Stream<Arguments> provideSequenceNumber() {
        return Stream.of(
                Arguments.of(0L, "000000"),
                Arguments.of(10L, "00000A"),
                Arguments.of(1000L, "0000RS"),
                Arguments.of(1664L, "0001A8"),
                Arguments.of(10000L, "0007PS"));
    }
}
