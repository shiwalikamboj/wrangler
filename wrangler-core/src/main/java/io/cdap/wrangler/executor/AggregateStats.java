package io.cdap.wrangler.executor;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AggregateStats implements Directive {
    public static final String NAME = "aggregate-stats";
    private String byteSizeCol;
    private String timeDurationCol;
    private String totalSizeCol;
    private String totalTimeCol;
    private long totalBytes = 0;
    private long totalNanos = 0;
    private int rowCount = 0;

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("byteSizeCol", TokenType.COLUMN_NAME);
        builder.define("timeDurationCol", TokenType.COLUMN_NAME);
        builder.define("totalSizeCol", TokenType.COLUMN_NAME);
        builder.define("totalTimeCol", TokenType.COLUMN_NAME);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.byteSizeCol = ((ColumnName) args.value("byteSizeCol")).value();
        this.timeDurationCol = ((ColumnName) args.value("timeDurationCol")).value();
        this.totalSizeCol = ((ColumnName) args.value("totalSizeCol")).value();
        this.totalTimeCol = ((ColumnName) args.value("totalTimeCol")).value();
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        for (Row row : rows) {
            Object sizeObj = row.getValue(byteSizeCol);
            Object timeObj = row.getValue(timeDurationCol);

            if (sizeObj instanceof ByteSize) {
                ByteSize byteSize = (ByteSize) sizeObj;
                totalBytes += byteSize.getBytes();
            }

            if (timeObj instanceof TimeDuration) {
                TimeDuration timeDuration = (TimeDuration) timeObj;
                String durationStr = timeDuration.toString();
                totalNanos += parseTimeDuration(durationStr);
            }

            rowCount++;
        }

        Row resultRow = new Row();
        resultRow.add(totalSizeCol, formatBytes(totalBytes));
        resultRow.add(totalTimeCol, formatDuration(totalNanos));

        List<Row> results = new ArrayList<>();
        results.add(resultRow);
        return results;
    }

    private long parseTimeDuration(String duration) {
        String value = duration.replaceAll("[^\\d.]", "");
        String unit = duration.replaceAll("[\\d.]", "").trim();
        double numericValue = Double.parseDouble(value);
        
        switch (unit.toLowerCase()) {
            case "ns":
                return (long) numericValue;
            case "us":
                return (long) (numericValue * 1000);
            case "ms":
                return (long) (numericValue * 1_000_000);
            case "s":
                return (long) (numericValue * 1_000_000_000);
            case "m":
                return (long) (numericValue * 60 * 1_000_000_000);
            case "h":
                return (long) (numericValue * 3600 * 1_000_000_000);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }

    private String formatDuration(long nanos) {
        long seconds = nanos / 1_000_000_000;
        if (seconds < 60) return seconds + " s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " m";
        long hours = minutes / 60;
        return hours + " h";
    }

    @Override
    public void destroy() {
        totalBytes = 0;
        totalNanos = 0;
        rowCount = 0;
    }
}
