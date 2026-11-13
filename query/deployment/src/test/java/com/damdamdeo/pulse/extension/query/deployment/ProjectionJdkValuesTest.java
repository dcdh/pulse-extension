package com.damdamdeo.pulse.extension.query.deployment;

import com.damdamdeo.pulse.extension.core.AggregateId;
import com.damdamdeo.pulse.extension.core.query.Projection;
import com.damdamdeo.pulse.extension.core.query.ProjectionId;
import io.quarkus.test.QuarkusUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectionJdkValuesTest {

    @RegisterExtension
    static final QuarkusUnitTest application = new QuarkusUnitTest()
            .withApplicationRoot(archive -> archive.addClasses(TestId.class, TestProjection.class, Amount.class)
                    .addClass(captured(java.util.List.of(1)).getClass()))
            .withConfigurationResource("application.properties");

    public record TestId(String id) implements AggregateId {
    }

    public record Amount(BigDecimal decimal, BigInteger integer) {
    }

    public record TestProjection(ProjectionId<TestId> id, Amount amount, LocalDate date, Month month)
            implements Projection<TestId> {
    }

    @Test
    void acceptsJdkValueTypesInsideProjectionRecords() {
        final var projection = new TestProjection(ProjectionId.from(new TestId("1")),
                new Amount(new BigDecimal("42.50"), BigInteger.TEN), LocalDate.of(2026, 11, 12), Month.NOVEMBER);
        assertEquals(new BigDecimal("42.50"), projection.amount().decimal());
    }

    static Projection<TestId> captured(final java.util.List<Integer> values) {
        return new Projection<>() {
            @Override
            public ProjectionId<TestId> id() {
                return ProjectionId.from(new TestId(values.getFirst().toString()));
            }
        };
    }

    @Test
    void ignoresCompilerGeneratedCaptures() {
        assertEquals("1", captured(java.util.List.of(1)).id().linkTo().id());
    }
}
