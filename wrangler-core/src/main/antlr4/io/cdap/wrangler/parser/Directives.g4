grammar Directives;

options {
  language = Java;
}

@lexer::header {
/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
}

// ===================================
// Parser Rules
// ===================================
byteSizeLiteral: BYTE_SIZE;
timeDurationLiteral: TIME_DURATION;

recipe
 : statements EOF
 ;

statements
  : ( COMMENT | macro | directive SEMI | pragma SEMI | ifStatement )*
;


directive
 : command
   ( codeblock
   | identifier
   | macro
   | text
   | number
   | bool
   | column
   | colList
   | numberList
   | boolList
   | stringList
   | numberRanges
   | properties
   | byteSize
   | timeDuration
   )*?
 ;

ifStatement
 : ifStat elseIfStat* elseStat? CBRACE
 ;

ifStat
 : IF expression OBRACE statements
 ;

elseIfStat
 : CBRACE ELSE IF expression OBRACE statements
 ;

elseStat
 : CBRACE ELSE OBRACE statements
 ;

expression
 : OPAREN (~OPAREN | expression)* CPAREN
 ;

forStatement
 : FOR OPAREN Identifier ASSIGN expression SEMI expression SEMI expression CPAREN OBRACE statements CBRACE
 ;

macro
 : DOLLAR OBRACE (~OBRACE | macro | Macro)*? CBRACE
 ;

pragma
 : PRAGMA (pragmaLoadDirective | pragmaVersion)
 ;

pragmaLoadDirective
 : LOAD_DIRECTIVES identifierList
 ;

pragmaVersion
 : VERSION Number
 ;

codeblock
 : EXP SPACE* COLON condition
 ;

identifier
 : Identifier
 ;

properties
 : PROP COLON OBRACE (propertyList)+ CBRACE
 | PROP COLON OBRACE OBRACE (propertyList)+ CBRACE { notifyErrorListeners("Too many start parentheses"); }
 | PROP COLON OBRACE (propertyList)+ CBRACE CBRACE { notifyErrorListeners("Too many end parentheses"); }
 | PROP COLON (propertyList)+ CBRACE { notifyErrorListeners("Missing opening brace"); }
 | PROP COLON OBRACE (propertyList)+ { notifyErrorListeners("Missing closing brace"); }
 ;

propertyList
 : property (COMMA property)*
 ;

property
 : Identifier ASSIGN value
 ;

numberRanges
 : numberRange (COMMA numberRange)*
 ;

numberRange
 : Number COLON Number ASSIGN value
 ;

value
 : text           #textValue
 | number         #numberValue
 | bool           #boolValue
 | byteSize       #byteSizeValue
 | timeDuration   #timeDurationValue
 | Identifier     #identifierValue
 | column         #columnValue
 ;

byteSize
 : BYTE_SIZE
 ;

timeDuration
 : TIME_DURATION
 ;

ecommand
 : EXCLAMATION Identifier
 ;

config
 : Identifier
 ;

column
 : Column
 ;

text
 : String
 ;

number
 : Number
 ;

bool
 : Bool
 ;

condition
 : OBRACE (~CBRACE | condition)* CBRACE
 ;

command
 : Identifier
 ;

colList
 : Column (COMMA Column)+
 ;

numberList
 : Number (COMMA Number)+
 ;

boolList
 : Bool (COMMA Bool)+
 ;

stringList
 : String (COMMA String)+
 ;

identifierList
 : Identifier (COMMA Identifier)*
 ;

// ===================================
// Lexer Rules
// ===================================

SEMI     : ';';
OBRACE   : '{';
CBRACE   : '}';
OPAREN   : '(';
CPAREN   : ')';
COMMA    : ',';
COLON    : ':';
ASSIGN   : '=';
DOLLAR   : '$';
EXCLAMATION : '!';

IF     : 'if';
ELSE   : 'else';
FOR    : 'for';
PROP   : 'prop';
EXP    : 'exp';
PRAGMA : '#pragma';
LOAD_DIRECTIVES : 'load-directives';
VERSION : 'version';

Bool : 'true' | 'false';

Number : Int ('.' Digit*)?;

fragment BYTE_UNIT : [KMGTP]? 'i'? 'B';
BYTE_SIZE : Number SPACE* BYTE_UNIT;

fragment TIME_UNIT : ('ns'|'ms'|'s'|'m'|'h'|'d');
TIME_DURATION : Number SPACE* TIME_UNIT;

Identifier : [a-zA-Z_\-] [a-zA-Z_0-9\-]*;
Macro      : [a-zA-Z_] [a-zA-Z_0-9]*;
Column     : ':' [a-zA-Z_\-] [a-zA-Z_0-9\-]*;

String
 : '\'' (EscapeSequence | ~('\''))* '\''
 | '"'  (EscapeSequence | ~('"'))* '"'
;

EscapeSequence
 : '\\' ('b'|'t'|'n'|'f'|'r'|'"'|'\''|'\\')
 | UnicodeEscape
 | OctalEscape
;

fragment OctalEscape
 : '\\' ('0'..'3') ('0'..'7') ('0'..'7')
 | '\\' ('0'..'7') ('0'..'7')
 | '\\' ('0'..'7')
;

fragment UnicodeEscape
 : '\\' 'u' HexDigit HexDigit HexDigit HexDigit
;

fragment HexDigit : ('0'..'9'|'a'..'f'|'A'..'F');

COMMENT
 : ('//' ~[\r\n]*
   | '/*' .*? '*/'
   | '--' ~[\r\n]*
   ) -> skip
;

SPACE : [ \t\r\n\u000C]+ -> skip;

fragment Int : '-'? [1-9] Digit* [L]* | '0';
fragment Digit : [0-9];
