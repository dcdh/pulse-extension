package com.damdamdeo.pulse.extension.build.report.deployment.content;

public interface Visitor {

    void visit(Title title);

    void visit(Paragraph paragraph);

    void visit(CodeBlock codeBlock);

    void visit(Admonition admonition);

    void visit(BasicList basicList);

    void visit(BasicTable basicTable);
}
