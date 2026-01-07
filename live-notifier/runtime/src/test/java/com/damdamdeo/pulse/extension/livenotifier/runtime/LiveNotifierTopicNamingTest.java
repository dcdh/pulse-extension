package com.damdamdeo.pulse.extension.livenotifier.runtime;

import com.damdamdeo.pulse.extension.core.consumer.FromApplication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiveNotifierTopicNamingTest {

    @Test
    void shouldGenerateName() {
        // Given
        final FromApplication givenFromApplication = new FromApplication("TodoTaking", "Todo");

        // When
        final LiveNotifierTopicNaming liveNotifierTopicNaming = new LiveNotifierTopicNaming(givenFromApplication);

        // Then
        assertThat(liveNotifierTopicNaming.name()).isEqualTo("pulse.live-notification.todotaking_todo");
    }
}
