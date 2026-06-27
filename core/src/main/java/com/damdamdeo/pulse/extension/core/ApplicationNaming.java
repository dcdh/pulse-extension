package com.damdamdeo.pulse.extension.core;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ApplicationNaming(String name) {

    public final static Pattern UPPER_CAMEL_CASE_PATTERN = Pattern.compile("^(?:[A-Z][a-z0-9]+){2,}$");
    private static final Pattern ELEMENTS_PATTERN = Pattern.compile("[A-Z][a-z0-9]*");

    public ApplicationNaming {
        Objects.requireNonNull(name);
        Validate.validState(UPPER_CAMEL_CASE_PATTERN.matcher(name).matches());
    }

    public static ApplicationNaming of(final String name) {
        return new ApplicationNaming(name);
    }

    public List<String> split() {
        List<String> elements = new ArrayList<>();
        final Matcher matcher = ELEMENTS_PATTERN.matcher(name);
        while (matcher.find()) {
            elements.add(matcher.group());
        }
        return elements;
    }

    public String functionalDomain() {
        return split().getFirst();
    }
}
