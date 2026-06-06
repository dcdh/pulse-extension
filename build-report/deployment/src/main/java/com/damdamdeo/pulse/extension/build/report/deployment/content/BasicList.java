package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.List;
import java.util.Objects;

public record BasicList(String title, List<ListItem> listItems) implements Content {

    public BasicList {
        // title can be null
        Objects.requireNonNull(listItems);
    }

    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
}
