package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.ApplicationNaming;
import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiveNotifierTopicNamingTest {

    @Test
    void shouldGenerateName() {
        // Given
        final FromApplication givenFromApplication = new FromApplication(new ApplicationNaming("TodoTaking"));

        // When
        final LiveNotifierTopicNaming liveNotifierTopicNaming = new LiveNotifierTopicNaming(givenFromApplication);

        // Then
        assertThat(liveNotifierTopicNaming.name()).isEqualTo("pulse.live-notification.todo_taking");
    }
}
