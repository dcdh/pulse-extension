package com.damdamdeo.pulse.extension.core.consumer;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

public record FromApplication(String functionalDomain, String componentName) {

    private final static String SEPARATOR = "_";
    public final static Pattern PART_PATTERN = Pattern.compile("^[a-zA-Z]{1,64}$");
    public final static Pattern FULL_PATTERN = Pattern.compile("^[a-zA-Z]{1,64}_[a-zA-Z]{1,64}$");

    public FromApplication {
        Objects.requireNonNull(functionalDomain);
        Validate.validState(PART_PATTERN.matcher(functionalDomain).matches());
        Objects.requireNonNull(componentName);
        Validate.validState(PART_PATTERN.matcher(componentName).matches());
    }

    public static FromApplication of(final String functionalDomain, final String componentName) {
        return new FromApplication(functionalDomain, componentName);
    }

    public static FromApplication from(final String applicationName) {
        Objects.requireNonNull(applicationName);
        Validate.validState(FULL_PATTERN.matcher(applicationName).matches());
        final String[] split = applicationName.split("_");
        return new FromApplication(split[0], split[1]);
    }

    public String value() {
        return functionalDomain + SEPARATOR + componentName;
    }
}
