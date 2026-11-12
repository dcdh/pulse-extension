package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DescriptionTest {

    @Test
    void shouldWrapDescription() {
        assertThat(new Description("lorem ipsum").description()).isEqualTo("lorem ipsum");
    }

    @Test
    void shouldRejectNullDescription() {
        assertThatNullPointerException().isThrownBy(() -> new Description(null));
    }
}
