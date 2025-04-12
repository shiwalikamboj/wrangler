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
  * A class that represents a byte size value with units (e.g., 10KB, 1.5MB).
  */
 @PublicEvolving
 public class ByteSize implements Token {
     private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("^([\\d.]+)\\s*([KMGTP]?i?B)$", Pattern.CASE_INSENSITIVE);
     private static final long KB = 1024L;
     private static final long MB = KB * KB;
     private static final long GB = MB * KB;
     private static final long TB = GB * KB;
     private static final long PB = TB * KB;
 
     private final long bytes;
     private final String rawValue;
 
     public ByteSize(String value) {
         this.rawValue = value.trim();
         this.bytes = parseByteSize(this.rawValue);
     }
 
     @Override
     public TokenType type() {
         return TokenType.BYTE_SIZE;
     }
 
     @Override
     public Object value() {  // Should return Object, not String
         return bytes;
     }
 
     @Override
     public JsonElement toJson() {
         JsonObject json = new JsonObject();
         json.addProperty("type", "BYTE_SIZE");
         json.addProperty("value", bytes);
         json.addProperty("original", rawValue);
         return json;
     }
 
    
     public String getOriginal() {
         return rawValue;
     }
 
     public long getBytes() {
         return bytes;
     }
 
     public double getKilobytes() {
         return bytes / (double) KB;
     }
 
     public double getMegabytes() {
         return bytes / (double) MB;
     }
 
     public double getGigabytes() {
         return bytes / (double) GB;
     }
 
     public double getTerabytes() {
         return bytes / (double) TB;
     }
 
     public double getPetabytes() {
         return bytes / (double) PB;
     }
 
     private long parseByteSize(String str) {
         Matcher matcher = BYTE_SIZE_PATTERN.matcher(str);
         if (!matcher.matches()) {
             throw new IllegalArgumentException(String.format(
                 "Invalid byte size format '%s'. Expected format is <number><unit> where unit is one of B, KB, MB, GB, TB, PB, KiB, MiB, GiB, TiB, PiB",
                 str));
         }
 
         double value = Double.parseDouble(matcher.group(1));
         String unit = matcher.group(2).toUpperCase();
 
         switch (unit) {
             case "B": return (long) value;
             case "KB":
             case "KIB": return (long) (value * KB);
             case "MB":
             case "MIB": return (long) (value * MB);
             case "GB":
             case "GIB": return (long) (value * GB);
             case "TB":
             case "TIB": return (long) (value * TB);
             case "PB":
             case "PIB": return (long) (value * PB);
             default: throw new IllegalArgumentException("Unknown byte size unit: " + unit);
         }
     }
 
     @Override
     public String toString() {
         if (bytes >= PB) return String.format("%.1fPB", getPetabytes());
         else if (bytes >= TB) return String.format("%.1fTB", getTerabytes());
         else if (bytes >= GB) return String.format("%.1fGB", getGigabytes());
         else if (bytes >= MB) return String.format("%.1fMB", getMegabytes());
         else if (bytes >= KB) return String.format("%.1fKB", getKilobytes());
         else return String.format("%dB", bytes);
     }
 }
