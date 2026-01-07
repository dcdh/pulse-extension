package com.damdamdeo.pulse.extension.core.consumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TopicNamingTest {

    @ParameterizedTest
    @CsvSource({
            "EVENT,pulse.todotaking_todo.event",
            "AGGREGATE_ROOT,pulse.todotaking_todo.aggregate_root"})
    void shouldGenerateName(final Table givenTable, final String expectedNaming) {
        // Given
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");

        // When
        final CdcTopicNaming cdcTopicNaming = new CdcTopicNaming(givenFromApplication, givenTable);

        // Then
        assertThat(cdcTopicNaming.name()).isEqualTo(expectedNaming);
    }
}
