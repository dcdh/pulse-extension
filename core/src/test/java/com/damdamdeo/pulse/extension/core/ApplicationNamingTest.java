package com.damdamdeo.pulse.extension.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationNamingTest {

    ApplicationNaming givenApplicationNaming = new ApplicationNaming("CustomerOrderLine");

    @Test
    void shouldSplit() {
        assertThat(givenApplicationNaming.split()).containsExactly("Customer", "Order", "Line");
    }

    @Test
    void shouldExtractFunctionalDomain() {
        assertThat(givenApplicationNaming.functionalDomain()).isEqualTo("Customer");
    }
}
