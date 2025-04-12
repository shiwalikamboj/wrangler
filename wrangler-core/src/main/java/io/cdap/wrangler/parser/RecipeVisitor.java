// package io.cdap.wrangler.parser;

// import io.cdap.wrangler.api.LazyNumber;
// import io.cdap.wrangler.api.RecipeSymbol;
// import io.cdap.wrangler.api.SourceInfo;
// import io.cdap.wrangler.api.parser.*;
// import org.antlr.v4.runtime.ParserRuleContext;
// import org.antlr.v4.runtime.misc.Interval;
// import org.antlr.v4.runtime.tree.TerminalNode;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// public final class RecipeVisitor extends DirectivesBaseVisitor<RecipeSymbol.Builder> {
//     private final RecipeSymbol.Builder builder = new RecipeSymbol.Builder();

//     public RecipeSymbol getCompiledUnit() {
//         return builder.build();
//     }

//     @Override
//     public RecipeSymbol.Builder visitDirective(DirectivesParser.DirectiveContext ctx) {
//         builder.createTokenGroup(getOriginalSource(ctx));
//         return super.visitDirective(ctx);
//     }

//     @Override
//     public RecipeSymbol.Builder visitPropertyList(DirectivesParser.PropertyListContext ctx) {
//         Map<String, Token> props = new HashMap<>();
//         for (DirectivesParser.PropertyContext property : ctx.property()) {
//             String id = property.Identifier().getText();
//             Token token = parsePropertyValue(property);
//             props.put(id, token);
//         }
//         builder.addToken(new Properties(props));
//         return builder;
//     }

//     private Token parsePropertyValue(DirectivesParser.PropertyContext property) {
//         // Property structure is: Identifier '=' value
//         // The value is the third child (index 2)
//         if (property.getChildCount() > 2) {
//             ParserRuleContext valueCtx = (ParserRuleContext) property.getChild(2);
            
//             // Use visitor pattern to handle the value context
//             if (valueCtx instanceof DirectivesParser.NumberContext) {
//                 return visitNumber((DirectivesParser.NumberContext) valueCtx).build().getTokens().get(0);
//             } else if (valueCtx instanceof DirectivesParser.BoolContext) {
//                 return visitBool((DirectivesParser.BoolContext) valueCtx).build().getTokens().get(0);
//             } else if (valueCtx instanceof DirectivesParser.TextContext) {
//                 return visitText((DirectivesParser.TextContext) valueCtx).build().getTokens().get(0);
//             } else if (valueCtx instanceof DirectivesParser.ByteSizeLiteralContext) {
//                 return visitByteSizeLiteral((DirectivesParser.ByteSizeLiteralContext) valueCtx).build().getTokens().get(0);
//             } else if (valueCtx instanceof DirectivesParser.TimeDurationLiteralContext) {
//                 return visitTimeDurationLiteral((DirectivesParser.TimeDurationLiteralContext) valueCtx).build().getTokens().get(0);
//             } else if (valueCtx instanceof DirectivesParser.IdentifierContext) {
//                 return visitIdentifier((DirectivesParser.IdentifierContext) valueCtx).build().getTokens().get(0);
//             } else if (valueCtx instanceof DirectivesParser.ColumnContext) {
//                 return new ColumnName(valueCtx.getText());
//             }
//         }
//         throw new IllegalArgumentException("Invalid property value: " + property.getText());
//     }

//     @Override
//     public RecipeSymbol.Builder visitNumber(DirectivesParser.NumberContext ctx) {
//         builder.addToken(new Numeric(new LazyNumber(ctx.getText())));
//         return builder;
//     }

//     @Override
//     public RecipeSymbol.Builder visitBool(DirectivesParser.BoolContext ctx) {
//         builder.addToken(new Bool(Boolean.parseBoolean(ctx.getText())));
//         return builder;
//     }

//     @Override
//     public RecipeSymbol.Builder visitText(DirectivesParser.TextContext ctx) {
//         String text = ctx.getText();
//         // Remove surrounding quotes
//         String content = text.substring(1, text.length() - 1);
//         builder.addToken(new Text(content));
//         return builder;
//     }

//     @Override
//     public RecipeSymbol.Builder visitByteSizeLiteral(DirectivesParser.ByteSizeLiteralContext ctx) {
//         builder.addToken(new ByteSize(ctx.getText()));
//         return builder;
//     }

//     @Override
//     public RecipeSymbol.Builder visitTimeDurationLiteral(DirectivesParser.TimeDurationLiteralContext ctx) {
//         builder.addToken(new TimeDuration(ctx.getText()));
//         return builder;
//     }

//     @Override
//     public RecipeSymbol.Builder visitIdentifier(DirectivesParser.IdentifierContext ctx) {
//         builder.addToken(new Identifier(ctx.getText()));
//         return builder;
//     }

//     @Override
//     public RecipeSymbol.Builder visitColumn(DirectivesParser.ColumnContext ctx) {
//         builder.addToken(new ColumnName(ctx.getText()));
//         return builder;
//     }

//     private SourceInfo getOriginalSource(ParserRuleContext ctx) {
//         int a = ctx.getStart().getStartIndex();
//         int b = ctx.getStop().getStopIndex();
//         Interval interval = new Interval(a, b);
//         String text = ctx.start.getInputStream().getText(interval);
//         int lineno = ctx.getStart().getLine();
//         int column = ctx.getStart().getCharPositionInLine();
//         return new SourceInfo(lineno, column, text);
//     }
// }
package io.cdap.wrangler.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

import io.cdap.wrangler.api.LazyNumber;
import io.cdap.wrangler.api.RecipeSymbol;
import io.cdap.wrangler.api.SourceInfo;
import io.cdap.wrangler.api.parser.Bool;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Identifier;
import io.cdap.wrangler.api.parser.Numeric;
import io.cdap.wrangler.api.parser.Properties;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.Token;

public final class RecipeVisitor extends DirectivesBaseVisitor<RecipeSymbol.Builder> {
    private final RecipeSymbol.Builder builder = new RecipeSymbol.Builder();

    public RecipeSymbol getCompiledUnit() {
        return builder.build();
    }

    @Override
    public RecipeSymbol.Builder visitDirective(DirectivesParser.DirectiveContext ctx) {
        Objects.requireNonNull(ctx, "Directive context cannot be null");
        builder.createTokenGroup(getOriginalSource(ctx));
        return super.visitDirective(ctx);
    }

    @Override
    public RecipeSymbol.Builder visitPropertyList(DirectivesParser.PropertyListContext ctx) {
        Objects.requireNonNull(ctx, "Property list context cannot be null");
        Map<String, Token> props = new HashMap<>();
        
        for (DirectivesParser.PropertyContext property : ctx.property()) {
            String id = property.Identifier().getText();
            Token token = parsePropertyValue(property);
            props.put(id, token);
        }
        
        builder.addToken(new Properties(props));
        return builder;
    }

    private Token parsePropertyValue(DirectivesParser.PropertyContext property) {
        Objects.requireNonNull(property, "Property context cannot be null");
        
        if (property.getChildCount() <= 2) {
            throw new IllegalArgumentException("Invalid property value: " + property.getText());
        }

        ParserRuleContext valueCtx = (ParserRuleContext) property.getChild(2);
        
        // Instead of trying to get tokens from the builder, create tokens directly
        if (valueCtx instanceof DirectivesParser.NumberContext) {
            return new Numeric(new LazyNumber(valueCtx.getText()));
        } else if (valueCtx instanceof DirectivesParser.BoolContext) {
            return new Bool(Boolean.parseBoolean(valueCtx.getText()));
        } else if (valueCtx instanceof DirectivesParser.TextContext) {
            String text = valueCtx.getText();
            String content = text.substring(1, text.length() - 1)
                               .replace("\\\"", "\"")
                               .replace("\\\\", "\\");
            return new Text(content);
        } else if (valueCtx instanceof DirectivesParser.ByteSizeLiteralContext) {
            return new ByteSize(valueCtx.getText());
        } else if (valueCtx instanceof DirectivesParser.TimeDurationLiteralContext) {
            return new TimeDuration(valueCtx.getText());
        } else if (valueCtx instanceof DirectivesParser.IdentifierContext) {
            return new Identifier(valueCtx.getText());
        } else if (valueCtx instanceof DirectivesParser.ColumnContext) {
            return new ColumnName(valueCtx.getText());
        }
        
        throw new IllegalArgumentException("Unsupported property value type: " + valueCtx.getClass().getSimpleName());
    }

    @Override
    public RecipeSymbol.Builder visitNumber(DirectivesParser.NumberContext ctx) {
        Objects.requireNonNull(ctx, "Number context cannot be null");
        builder.addToken(new Numeric(new LazyNumber(ctx.getText())));
        return builder;
    }

    @Override
    public RecipeSymbol.Builder visitBool(DirectivesParser.BoolContext ctx) {
        Objects.requireNonNull(ctx, "Bool context cannot be null");
        builder.addToken(new Bool(Boolean.parseBoolean(ctx.getText())));
        return builder;
    }

    @Override
    public RecipeSymbol.Builder visitText(DirectivesParser.TextContext ctx) {
        Objects.requireNonNull(ctx, "Text context cannot be null");
        String text = ctx.getText();
        String content = text.substring(1, text.length() - 1)
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\");
        builder.addToken(new Text(content));
        return builder;
    }

    @Override
    public RecipeSymbol.Builder visitByteSizeLiteral(DirectivesParser.ByteSizeLiteralContext ctx) {
        Objects.requireNonNull(ctx, "ByteSizeLiteral context cannot be null");
        builder.addToken(new ByteSize(ctx.getText()));
        return builder;
    }

    @Override
    public RecipeSymbol.Builder visitTimeDurationLiteral(DirectivesParser.TimeDurationLiteralContext ctx) {
        Objects.requireNonNull(ctx, "TimeDurationLiteral context cannot be null");
        builder.addToken(new TimeDuration(ctx.getText()));
        return builder;
    }

    @Override
    public RecipeSymbol.Builder visitIdentifier(DirectivesParser.IdentifierContext ctx) {
        Objects.requireNonNull(ctx, "Identifier context cannot be null");
        builder.addToken(new Identifier(ctx.getText()));
        return builder;
    }

    @Override
    public RecipeSymbol.Builder visitColumn(DirectivesParser.ColumnContext ctx) {
        Objects.requireNonNull(ctx, "Column context cannot be null");
        builder.addToken(new ColumnName(ctx.getText()));
        return builder;
    }

    private SourceInfo getOriginalSource(ParserRuleContext ctx) {
        Objects.requireNonNull(ctx, "Context cannot be null");
        Objects.requireNonNull(ctx.getStart(), "Start token cannot be null");
        Objects.requireNonNull(ctx.getStop(), "Stop token cannot be null");
        
        int startIdx = ctx.getStart().getStartIndex();
        int stopIdx = ctx.getStop().getStopIndex();
        Interval interval = new Interval(startIdx, stopIdx);
        
        String text = ctx.start.getInputStream().getText(interval);
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        
        return new SourceInfo(line, column, text);
    }
}
