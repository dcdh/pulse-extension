package com.damdamdeo.pulse.extension.livenotifier.deployment;

import com.damdamdeo.pulse.extension.livenotifier.TopicManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractMessagingTest {

    @BeforeEach
    @AfterEach
    void purgeTopics() {
        TopicManager.resetTopics();
    }
}
