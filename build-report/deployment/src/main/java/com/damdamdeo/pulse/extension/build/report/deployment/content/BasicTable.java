package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.List;
import java.util.Objects;

public record BasicTable(List<TableRow> tableRows) implements Content {

    public BasicTable {
        Objects.requireNonNull(tableRows);
        if (!tableRows.isEmpty()) {
            final int expectedColumnCount = tableRows.getFirst().content().size();
            for (final TableRow row : tableRows) {
                if (row.content().size() != expectedColumnCount) {
                    throw new IllegalArgumentException(
                            "All table rows must contain the same number of columns. " +
                                    "Expected " + expectedColumnCount +
                                    " but found " + row.content().size()
                    );
                }
            }
        }
    }

    public boolean isEmpty() {
        return tableRows.isEmpty();
    }

    @Override
    public void accept(final Visitor visitor) {
        visitor.visit(this);
    }
}
