package io.cdap.wrangler.executor;

import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.executor.TestingRig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for AggregateStats directive
 */
@RunWith(JUnit4.class)
public class AggregateStatsDirectiveTest {
    private AggregateStats directive;

    @Before
    public void setUp() {
        directive = new AggregateStats();
    }

    @Test
    public void testBasicAggregation() throws Exception {
        List<Row> rows = Arrays.asList(
            new Row().add("data_size", new ByteSize("100MB"))
                    .add("response_time", new TimeDuration("500ms")),
            new Row().add("data_size", new ByteSize("200MB"))
                    .add("response_time", new TimeDuration("1.5s"))
        );

        String[] directives = new String[] {
            "aggregate-stats :data_size :response_time total_size total_time"
        };

        List<Row> results = TestingRig.execute(directives, rows);

        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        Assert.assertEquals("300.00 MB", result.getValue("total_size"));
        Assert.assertEquals("2 s", result.getValue("total_time"));
    }

    @Test
    public void testMixedUnits() throws Exception {
        List<Row> rows = Arrays.asList(
            new Row().add("data_size", new ByteSize("1GB"))
                    .add("response_time", new TimeDuration("30m")),
            new Row().add("data_size", new ByteSize("500MB"))
                    .add("response_time", new TimeDuration("1h"))
        );

        String[] directives = new String[] {
            "aggregate-stats :data_size :response_time total_size total_time"
        };

        List<Row> results = TestingRig.execute(directives, rows);

        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        Assert.assertEquals("1.49 GB", result.getValue("total_size"));
        Assert.assertEquals("1 h", result.getValue("total_time"));
    }

    @Test
    public void testLargeValues() throws Exception {
        List<Row> rows = Arrays.asList(
            new Row().add("data_size", new ByteSize("2TB"))
                    .add("response_time", new TimeDuration("24h")),
            new Row().add("data_size", new ByteSize("1.5TB"))
                    .add("response_time", new TimeDuration("48h"))
        );

        String[] directives = new String[] {
            "aggregate-stats :data_size :response_time total_size total_time"
        };

        List<Row> results = TestingRig.execute(directives, rows);

        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        Assert.assertEquals("3.50 TB", result.getValue("total_size"));
        Assert.assertEquals("72 h", result.getValue("total_time"));
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testMissingColumns() throws Exception {
        List<Row> rows = Arrays.asList(
            new Row().add("wrong_column", new ByteSize("100MB"))
        );

        String[] directives = new String[] {
            "aggregate-stats :data_size :response_time total_size total_time"
        };

        TestingRig.execute(directives, rows);
    }

    @Test
    public void testEmptyInput() throws Exception {
        List<Row> rows = new ArrayList<>();
        
        String[] directives = new String[] {
            "aggregate-stats :data_size :response_time total_size total_time"
        };

        List<Row> results = TestingRig.execute(directives, rows);

        Assert.assertEquals(1, results.size());
        Row result = results.get(0);
        Assert.assertEquals("0 B", result.getValue("total_size"));
        Assert.assertEquals("0 s", result.getValue("total_time"));
    }

    @Test(expected = DirectiveExecutionException.class)
    public void testInvalidDataTypes() throws Exception {
        List<Row> rows = Arrays.asList(
            new Row().add("data_size", "invalid_size")
                    .add("response_time", "invalid_time")
        );

        String[] directives = new String[] {
            "aggregate-stats :data_size :response_time total_size total_time"
        };

        TestingRig.execute(directives, rows);
    }
}
