package com.damdamdeo.pulse.extension.build.report.deployment;

import com.damdamdeo.pulse.extension.build.report.deployment.content.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AsciiDoctorGeneratorTest {

    @Test
    void shouldGenerateExpectedDoc() {
        // Given
        final List<Content> givenContents = List.of(
                new Title(Title.Level.FIRST, "Guide Quarkus"),
                new Paragraph("Introduction."),
                new Admonition(
                        AdmonitionType.NOTE,
                        "Cette extension est expérimentale."),
                new CodeBlock(
                        "java",
                        """
                                public class Hello {
                                }
                                """),
                new BasicList(null,
                        List.of(new ListItem("Edgar Allan Poe"),
                                new ListItem("Sheri S. Tepper"),
                                new ListItem("Bill Bryson"))),
                new BasicTable(
                        List.of(
                                new TableRow(List.of("Cell in column 1, row 1", "Cell in column 2, row 1")),
                                new TableRow(List.of("Cell in column 1, row 2", "Cell in column 2, row 2")),
                                new TableRow(List.of("Cell in column 1, row 3", "Cell in column 2, row 3")))));

        // When
        final String generated = AsciiDoctorGenerator.generate(givenContents);

        // Then
        assertThat(generated).isEqualTo(
                // language=ad
                """
                        = Guide Quarkus
                        :toc: left
                        :toclevels: 3
                        
                        Introduction.
                        
                        [NOTE]
                        ====
                        Cette extension est expérimentale.
                        ====
                        
                        [source,java]
                        ----
                        public class Hello {
                        }
                        ----
                        
                        * Edgar Allan Poe
                        * Sheri S. Tepper
                        * Bill Bryson
                        
                        [cols="1,1"]
                        |===
                        |Cell in column 1, row 1
                        |Cell in column 2, row 1
                        |Cell in column 1, row 2
                        |Cell in column 2, row 2
                        |Cell in column 1, row 3
                        |Cell in column 2, row 3
                        |===
                        
                        """);
    }
}
