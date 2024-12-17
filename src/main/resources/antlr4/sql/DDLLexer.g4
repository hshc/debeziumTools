lexer grammar DDLLexer;
@header {
package fr.hshc.db.antlr4;
}
/*
 * Lexer Rules
 */



WHITE_SPACE : [ \t\r\n]+ -> skip;
CREATE 		: ('CREATE'(' ' | '\t' | '\r' | '\n')+);
TABLE 		: ('TABLE'(' ' | '\t' | '\r' | '\n')+) -> pushMode(inCreateTable);

mode inCreateTable;
WS          : [ \t\r\n]+ -> skip;
LR_BRACKET 	: '(';

NUMBERS    : [0-9]+;
LETTERS     : ([a-zA-Z] | [0-9])+;

DOT			: '.';
RR_BRACKET 	: ')';
SEMI_COL	: (' ' | '\t' | '\r' | '\n')*(';') -> popMode;

UNDERLINE	: '_';
NEWLINE 	: [\r\n]+ ;
COMMA 		: ',';
