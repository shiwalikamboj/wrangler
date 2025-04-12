/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

 package io.cdap.wrangler.parser;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;

 import com.google.gson.JsonElement;
 import com.google.gson.JsonObject;

 import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.LazyNumber;
import io.cdap.wrangler.api.TokenGroup;
import io.cdap.wrangler.api.parser.Bool;
import io.cdap.wrangler.api.parser.BoolList;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.ColumnNameList;
import io.cdap.wrangler.api.parser.Numeric;
import io.cdap.wrangler.api.parser.NumericList;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TextList;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenDefinition;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
 
 /**
  * Implements the Arguments interface by mapping argument names to parsed tokens.
  */
 public class MapArguments implements Arguments {
   private final Map<String, Token> tokens;
   private final int lineno;
   private final int columnno;
   private final String source;
 
   public MapArguments(UsageDefinition definition, TokenGroup group) throws DirectiveParseException {
     this.tokens = new HashMap<>();
     this.lineno = group.getSourceInfo().getLineNumber();
     this.columnno = group.getSourceInfo().getColumnNumber();
     this.source = group.getSourceInfo().getSource();
 
     int required = definition.getTokens().size() - definition.getOptionalTokensCount();
     if ((required > group.size() - 1) || ((group.size() - 1) > definition.getTokens().size())) {
       throw new DirectiveParseException(
         definition.getDirectiveName(), String.format("Improper usage of directive '%s', usage - '%s'",
           definition.getDirectiveName(), definition.toString()));
     }
 
     List<TokenDefinition> specifications = definition.getTokens();
     Iterator<Token> it = group.iterator();
     int pos = 0;
     it.next(); // skip directive name
 
     while (it.hasNext()) {
       Token token = it.next();
       while (pos < specifications.size()) {
         TokenDefinition spec = specifications.get(pos);
 
         if (!spec.optional()) {
           if (!spec.type().equals(token.type())) {
             // attempt conversion if types are close enough
             TokenType expected = spec.type();
             TokenType actual = token.type();
 
             if (expected == TokenType.COLUMN_NAME_LIST && actual == TokenType.COLUMN_NAME) {
               List<String> values = new ArrayList<>();
               values.add(((ColumnName) token).value());
               tokens.put(spec.name(), new ColumnNameList(values));
               pos++;
               break;
             } else if (expected == TokenType.NUMERIC_LIST && actual == TokenType.NUMERIC) {
               List<LazyNumber> values = new ArrayList<>();
               values.add(((Numeric) token).value());
               tokens.put(spec.name(), new NumericList(values));
               pos++;
               break;
             } else if (expected == TokenType.BOOLEAN_LIST && actual == TokenType.BOOLEAN) {
               List<Boolean> values = new ArrayList<>();
               values.add(((Bool) token).value());
               tokens.put(spec.name(), new BoolList(values));
               pos++;
               break;
             } else if (expected == TokenType.TEXT_LIST && actual == TokenType.TEXT) {
               List<String> values = new ArrayList<>();
               values.add(((Text) token).value());
               tokens.put(spec.name(), new TextList(values));
               pos++;
               break;
             } else {
               throw new DirectiveParseException(
                 String.format("Expected argument '%s' to be of type '%s', but got '%s' instead. %s",
                   spec.name(), spec.type().name(), token.type().name(), group.getSourceInfo().toString()));
             }
           } else {
             tokens.put(spec.name(), token);
             pos++;
             break;
           }
         } else {
           pos++;
           if (spec.type().equals(token.type())) {
             tokens.put(spec.name(), token);
             break;
           }
         }
       }
     }
   }
 
   @Override
   public int size() {
     return tokens.size();
   }
 
   @Override
   public boolean contains(String name) {
     return tokens.containsKey(name);
   }
 
   @Override
   public <T extends Token> T value(String name) {
     return (T) tokens.get(name);
   }
 
   @Override
   public TokenType type(String name) {
     return tokens.get(name).type();
   }
 
   @Override
   public int line() {
     return lineno;
   }
 
   @Override
   public int column() {
     return columnno;
   }
 
   @Override
   public String source() {
     return source;
   }
 
   @Override
   public JsonElement toJson() {
     JsonObject object = new JsonObject();
     JsonObject arguments = new JsonObject();
     for (Map.Entry<String, Token> entry : tokens.entrySet()) {
       arguments.add(entry.getKey(), entry.getValue().toJson());
     }
     object.addProperty("line", lineno);
     object.addProperty("column", columnno);
     object.addProperty("source", source);
     object.add("arguments", arguments);
     return object;
   }
 
  // @Override
   public long asLong(String name) {
     Token token = tokens.get(name);
     if (token == null) {
       throw new IllegalArgumentException(String.format("Argument '%s' not found.", name));
     }
 
     if (token instanceof Numeric) {
       return ((Numeric) token).value().longValue();
     } else if (token instanceof ByteSize) {
       return ((ByteSize) token).getBytes();
     } else if (token instanceof TimeDuration) {
       return (long) ((TimeDuration) token).getMilliseconds(); // Explicit cast to long
     } else {
       throw new IllegalArgumentException(String.format(
         "Cannot convert argument '%s' of type '%s' to long.", name, token.type().name()));
     }
   }
 
  // @Override
   public String asString(String name) {
     Token token = tokens.get(name);
     if (token == null) {
       throw new IllegalArgumentException(String.format("Argument '%s' not found.", name));
     }
     return token.value().toString(); // Explicit toString() conversion
   }
 
   /**
    * Returns the double value of a numeric-compatible token.
    * For Numeric: returns the double value
    * For ByteSize: returns value in bytes as double
    * For TimeDuration: returns value in milliseconds as double
    */
   public double asDouble(String name) {
     Token token = tokens.get(name);
     if (token == null) {
       throw new IllegalArgumentException(String.format("Argument '%s' not found.", name));
     }
 
     if (token instanceof Numeric) {
       return ((Numeric) token).value().doubleValue();
     } else if (token instanceof ByteSize) {
       return (double) ((ByteSize) token).getBytes();
     } else if (token instanceof TimeDuration) {
       return ((TimeDuration) token).getMilliseconds();
     } else {
       throw new IllegalArgumentException(String.format(
         "Cannot convert argument '%s' of type '%s' to double.", name, token.type().name()));
     }
   }
 
   /**
    * Gets a ByteSize token value directly.
    */
   public ByteSize asByteSize(String name) {
     Token token = tokens.get(name);
     if (token == null) {
       throw new IllegalArgumentException(String.format("Argument '%s' not found.", name));
     }
     if (!(token instanceof ByteSize)) {
       throw new IllegalArgumentException(String.format(
         "Argument '%s' is not a ByteSize (was %s)", name, token.type().name()));
     }
     return (ByteSize) token;
   }
 
   /**
    * Gets a TimeDuration token value directly.
    */
   public TimeDuration asTimeDuration(String name) {
     Token token = tokens.get(name);
     if (token == null) {
       throw new IllegalArgumentException(String.format("Argument '%s' not found.", name));
     }
     if (!(token instanceof TimeDuration)) {
       throw new IllegalArgumentException(String.format(
         "Argument '%s' is not a TimeDuration (was %s)", name, token.type().name()));
     }
     return (TimeDuration) token;
   }
 }
