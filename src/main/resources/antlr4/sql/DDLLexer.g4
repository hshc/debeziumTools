lexer grammar DDLLexer;
@header {
package fr.hshc.db.antlr4;
}
/*
 * Lexer Rules
 */



WHITE_SPACE : [ \t\r\n]+;
CREATE 		: ('CREATE'(' ' | '\t' | '\r' | '\n')+);
TABLE 		: ('TABLE'(' ' | '\t' | '\r' | '\n')+) -> pushMode(inCreateTable);

mode inCreateTable;
WS          : [ \t\r\n];
LR_BRACKET 	: '(';

NUMBER    : [0-9];
LETTER     : [a-zA-Z];

DOT			: '.';
RR_BRACKET 	: ')';
SEMI_COL	: (' ' | '\t' | '\r' | '\n')*(';') -> popMode;
UNDERLINE	: '_';
COMMA 		: ',';
