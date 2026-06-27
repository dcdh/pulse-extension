package com.damdamdeo.pulse.extension.core.consumer;

import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TopicNamingTest {

    @ParameterizedTest
    @CsvSource({
            "EVENT,pulse.todo_taking.event",
            "AGGREGATE_ROOT,pulse.todo_taking.aggregate_root"})
    void shouldGenerateName(final Table givenTable, final String expectedNaming) {
        // Given
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));

        // When
        final CdcTopicNaming cdcTopicNaming = CdcTopicNaming.from(givenFromApplication, givenTable);

        // Then
        assertThat(cdcTopicNaming.name()).isEqualTo(expectedNaming);
    }
}
