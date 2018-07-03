/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

grammar Eval;

main: core tagEntry* reqEntry;

core: ID NUMBER ID*;

tagEntry: ID ASSIGN expr;
reqEntry: numberRange? ID selectionType? whereClause? ('{' reqEntry* '}')?;

numberRange:
        NUMBER                                                                        # ExactRange
        | '[' NUMBER ',' NUMBER ']'                                                   # BetweenRange
        ;

selectionType: EACH | TOTAL;

whereClause: WHERE expr;

// Operator precedence is copied from Java...
//   http://www.cs.bilkent.edu.tr/~guvenir/courses/CS101/op_precedence.html
expr:
        '(' expr ')'                                                                  # ExprBracket
        | <assoc=right> '!' expr                                                      # ExprNot
        | expr op=( '/' | '*' ) expr                                                  # ExprDivMul
        | expr op=( '+' | '-' ) expr                                                  # ExprAddSub
        | expr op=( '>' | '<' | '>=' | '<=' ) expr                                    # ExprRelational
        | expr op=( '==' | '!=' ) expr                                                # ExprEquality
        | expr AND expr                                                               # ExprAnd
        | expr OR expr                                                                # ExprOr
        | BOOLEAN                                                                     # ExprBoolean
        | NUMBER                                                                      # ExprNumber
        | STRING                                                                      # ExprString
        | ID '(' (expr ',')* expr? ')'                                                # ExprFunction
        | (ID '.')? ID                                                                # ExprVariable
        ;





WHERE: 'where';

EACH: 'each';
TOTAL: 'total';

BOOLEAN: 'true' | 'false';
NUMBER: '-'? [0-9]+ ('.' [0-9]+)? (( 'k' | 'm' | 'g' | 't' | 'p' | 'e' ) 'b'?)?;
STRING: '"' (STRING_ESC|.)*? '"';
fragment STRING_ESC : '\\"' | '\\\\';

ASSIGN: '=';
NOT: '!';
EQUALS: '==';
NOT_EQUALS: '!=';
GREATER_THAN: '>';
LESS_THAN: '<';
GREATER_THAN_OR_EQUALS: '>=';
LESS_THAN_OR_EQUALS: '<=';
AND: '&&';
OR: '||';

OPEN_BRACE: '{';
CLOSE_BRACE: '}';
OPEN_BRACKET: '(';
CLOSE_BRACKET: ')';
OPEN_SQUARE_BRACKET: '[';
CLOSE_SQUARE_BRACKET: ']';
DIVIDE: '/';
MULTIPLY: '*';
ADDITION: '+';
SUBTRACT: '-';

ID: [a-z0-9_]+;

WS: [ \t\r\n]+             -> channel(HIDDEN);
