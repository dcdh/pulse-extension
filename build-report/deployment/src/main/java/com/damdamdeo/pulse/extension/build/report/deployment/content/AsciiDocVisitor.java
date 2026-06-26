package com.damdamdeo.pulse.extension.build.report.deployment.content;

import java.util.Objects;

public class AsciiDocVisitor implements Visitor {

    private final StringBuilder builder = new StringBuilder();

    @Override
    public void visit(final Title title) {
        Objects.requireNonNull(title);
        builder.repeat("=", title.level())
                .append(" ")
                .append(title.content())
                .append("\n");
        if (title.level() == 1) {
            builder.append(":toc: left\n")
                    .append(":toclevels: 2\n");
        }
        builder.append("\n");
    }

    @Override
    public void visit(final Paragraph paragraph) {
        Objects.requireNonNull(paragraph);
        builder.append(paragraph.content())
                .append("\n\n");
    }

    @Override
    public void visit(final CodeBlock codeBlock) {
        Objects.requireNonNull(codeBlock);
        final String content = codeBlock.content();
        builder.append("[source,")
                .append(codeBlock.source())
                .append("]\n")
                .append("----\n")
                .append(content);
        if (!(content.endsWith("\n") || content.endsWith("\r"))) {
            builder.append("\n----\n\n");
        } else {
            builder.append("----\n\n");
        }
    }

    @Override
    public void visit(final Admonition admonition) {
        Objects.requireNonNull(admonition);
        builder.append("[")
                .append(admonition.admonitionType())
                .append("]\n")
                .append("====\n")
                .append(admonition.content())
                .append("\n====\n\n");
    }

    @Override
    public void visit(final BasicList basicList) {
        Objects.requireNonNull(basicList);
        if (basicList.title() != null) {
            builder.append(".").append(basicList.title());
        }
        if (!basicList.listItems().isEmpty()) {
            basicList.listItems().forEach(listItem -> builder.append("* ").append(listItem.content()).append("\n"));
            builder.append("\n");
        }
    }

    @Override
    public void visit(final BasicTable basicTable) {
        Objects.requireNonNull(basicTable);
        if (basicTable.tableRows().isEmpty()) {
            return;
        }
        final TableRow first = basicTable.tableRows().getFirst();
        final int columnCount = first.content().size();
        builder.append("[cols=\"");
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('1');
        }
        builder.append("\"]\n");
        builder.append("|===\n");
        for (final TableRow row : basicTable.tableRows()) {
            for (final String cell : row.content()) {
                builder.append('|').append(cell).append('\n');
            }
        }
        builder.append("|===\n\n");
    }

    public String render() {
        return builder.toString();
    }
}
