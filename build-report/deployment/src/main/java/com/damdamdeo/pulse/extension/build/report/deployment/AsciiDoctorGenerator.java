package com.damdamdeo.pulse.extension.build.report.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.content.AsciiDocVisitor;
import com.damdamdeo.pulse.extension.build.report.deployment.content.Content;

import java.util.List;
import java.util.Objects;

public class AsciiDoctorGenerator {

    public static String generate(final List<Content> contents) {
        Objects.requireNonNull(contents);
        AsciiDocVisitor visitor = new AsciiDocVisitor();
        contents.forEach(content -> content.accept(visitor));
        return visitor.render();
    }
}
