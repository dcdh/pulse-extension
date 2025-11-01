package com.damdamdeo.pulse.extension.core.consumer;

import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.regex.Pattern;

public record ApplicationNaming(String functionalDomain, String componentName) {

    private final static String SEPARATOR = "_";
    public final static Pattern PART_PATTERN = Pattern.compile("^[a-zA-Z]{1,64}$");
    public final static Pattern FULL_PATTERN = Pattern.compile("^[a-zA-Z]{1,64}_[a-zA-Z]{1,64}$");

    public ApplicationNaming {
        Objects.requireNonNull(functionalDomain);
        Validate.validState(PART_PATTERN.matcher(functionalDomain).matches());
        Objects.requireNonNull(componentName);
        Validate.validState(PART_PATTERN.matcher(componentName).matches());
    }

    public static ApplicationNaming of(final String functionalDomain, final String componentName) {
        return new ApplicationNaming(functionalDomain, componentName);
    }

    public static ApplicationNaming from(final String applicationName) {
        Objects.requireNonNull(applicationName);
        Validate.validState(FULL_PATTERN.matcher(applicationName).matches());
        final String[] split = applicationName.split("_");
        return new ApplicationNaming(split[0], split[1]);
    }

    public String value() {
        return functionalDomain + SEPARATOR + componentName;
    }
}
