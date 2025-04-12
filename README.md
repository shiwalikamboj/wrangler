CDAP Wrangler Enhancements
Added support for byte size (e.g., KB, MB) and time duration (e.g., ms, s) parsing in Wrangler recipes, plus a new aggregate-stats directive.
New Features

1. Quick Start
Add dependency (Maven):
<dependency>
  <groupId>io.cdap</groupId>
  <artifactId>wrangler-core</artifactId>
  <version>[LATEST_VERSION]</version>
</dependency>

2. Grammar Modification (Directives.g4)
   Add lexer rules
   Update parser rules
   
3. Implement Token Classes (wrangler-api)
3.1 Byte Size Parser
   Units: B, KB, MB, GB, TB (case-insensitive, binary base 1024)
   Java Usage:
ByteSize size = new ByteSize("1.5MB");
long bytes = size.getBytes();      // 1572864 bytes
double mb = size.toUnit("MB");     // 1.5 MB
3.2 Time Duration Parser
Units: ns, µs, ms, s, m, h
Java Usage:
TimeDuration duration = new TimeDuration("500ms");
long nanos = duration.getNanos();  // 500000000 ns
double sec = duration.toUnit("s"); // 0.5 seconds

4. Update Parser Logic (wrangler-core)
Add visitor methods
Register token types in TokenType.java.

5. AggregateStats Directive
Created the new directive AggregateStats.java under wrangler-core.
It implements the Directive interface and uses ExecutorContext to store intermediate results across rows.
During execution, it reads, parses, and converts size/time values to canonical units, performs accumulation, and finally outputs totals or averages as per the arguments.

6. Testing
 Unit tests (e.g., ByteSizeTest.java)
Integration test

Build & Test
mvn clean install

Supported Versions
Java 8+
CDAP Wrangler 6.7.0+

Time Estimate
Step	Time (Hours)
Setup & Grammar	2
Token Classes	3
Parser Updates	2
Directive Implementation	4
Testing	3
Documentation	1
Total	15
