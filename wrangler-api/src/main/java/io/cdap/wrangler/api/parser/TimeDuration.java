/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

 package io.cdap.wrangler.api.parser;

 import java.util.regex.Matcher;
 import java.util.regex.Pattern;

 import com.google.gson.JsonElement;
 import com.google.gson.JsonObject;

 import io.cdap.wrangler.api.annotations.PublicEvolving;
 
 /**
  * A class that represents a time duration with units (e.g., 100ms, 2.5s).
  */
 @PublicEvolving
 public class TimeDuration implements Token {
     private static final Pattern TIME_DURATION_PATTERN = 
         Pattern.compile("^([\\d.]+)\\s*([num]?s|ms|h|d)$", Pattern.CASE_INSENSITIVE);
     
     // Conversion constants (using nanoseconds as base unit)
     private static final long NANOS_PER_MICRO = 1000L;
     private static final long NANOS_PER_MILLI = 1000 * NANOS_PER_MICRO;
     private static final long NANOS_PER_SECOND = 1000 * NANOS_PER_MILLI;
     private static final long NANOS_PER_MINUTE = 60 * NANOS_PER_SECOND;
     private static final long NANOS_PER_HOUR = 60 * NANOS_PER_MINUTE;
     private static final long NANOS_PER_DAY = 24 * NANOS_PER_HOUR;
 
     private final long nanoseconds;
     private final String rawValue;
 
     public TimeDuration(String value) {
         this.rawValue = value.trim();
         this.nanoseconds = parseTimeDuration(this.rawValue);
     }
 
     @Override
     public TokenType type() {
         return TokenType.TIME_DURATION;
     }
 
     @Override
     public Object value() {
         return nanoseconds;
     }
 
     @Override
     public JsonElement toJson() {
         JsonObject json = new JsonObject();
         json.addProperty("type", "TIME_DURATION");
         json.addProperty("value", nanoseconds);
         json.addProperty("original", rawValue);
         return json;
     }
 
    
     public String getOriginal() {
         return rawValue;
     }
 
     // Conversion methods
     public long getNanoseconds() {
         return nanoseconds;
     }
 
     public double getMicroseconds() {
         return nanoseconds / (double) NANOS_PER_MICRO;
     }
 
     public double getMilliseconds() {
         return nanoseconds / (double) NANOS_PER_MILLI;
     }
 
     public double getSeconds() {
         return nanoseconds / (double) NANOS_PER_SECOND;
     }
 
     public double getMinutes() {
         return nanoseconds / (double) NANOS_PER_MINUTE;
     }
 
     public double getHours() {
         return nanoseconds / (double) NANOS_PER_HOUR;
     }
 
     public double getDays() {
         return nanoseconds / (double) NANOS_PER_DAY;
     }
 
     private long parseTimeDuration(String str) {
         Matcher matcher = TIME_DURATION_PATTERN.matcher(str);
         if (!matcher.matches()) {
             throw new IllegalArgumentException(String.format(
                 "Invalid time duration format '%s'. Expected format is <number><unit> where unit is one of ns, μs, ms, s, m, h, d",
                 str));
         }
 
         double value = Double.parseDouble(matcher.group(1));
         String unit = matcher.group(2).toLowerCase();
 
         switch (unit) {
             case "ns": return (long) value;
             case "μs": return (long) (value * NANOS_PER_MICRO);
             case "ms": return (long) (value * NANOS_PER_MILLI);
             case "s": return (long) (value * NANOS_PER_SECOND);
             case "m": return (long) (value * NANOS_PER_MINUTE);
             case "h": return (long) (value * NANOS_PER_HOUR);
             case "d": return (long) (value * NANOS_PER_DAY);
             default: throw new IllegalArgumentException("Unknown time unit: " + unit);
         }
     }
 
     @Override
     public String toString() {
         if (nanoseconds >= NANOS_PER_DAY) return String.format("%.1fd", getDays());
         else if (nanoseconds >= NANOS_PER_HOUR) return String.format("%.1fh", getHours());
         else if (nanoseconds >= NANOS_PER_MINUTE) return String.format("%.1fm", getMinutes());
         else if (nanoseconds >= NANOS_PER_SECOND) return String.format("%.1fs", getSeconds());
         else if (nanoseconds >= NANOS_PER_MILLI) return String.format("%.1fms", getMilliseconds());
         else if (nanoseconds >= NANOS_PER_MICRO) return String.format("%.1fμs", getMicroseconds());
         else return String.format("%dns", nanoseconds);
     }
 }
