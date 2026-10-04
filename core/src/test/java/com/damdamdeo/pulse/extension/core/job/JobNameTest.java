package com.damdamdeo.pulse.extension.core.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"Job", "scheduledJob", "DAILYJOB", "a", "Z"})
    void shouldCreateJobNameContainingOnlyAsciiLetters(final String name) {
        // Given

        // When
        final JobName jobName = new JobName(name);

        // Then
        assertEquals(name, jobName.name());
    }

    @Test
    void shouldRejectNullName() {
        assertThrows(NullPointerException.class, () -> new JobName(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Job1",
            "job-name",
            "job_name",
            "job name",
            " Job",
            "Job ",
            "Tâche"
    })
    void shouldRejectNameContainingCharactersOtherThanAsciiLetters(final String name) {
        // Given

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> new JobName(name));
    }
}
