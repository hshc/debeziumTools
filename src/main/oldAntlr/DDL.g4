//grammar DDL;
//@header {
//    package fr.hshc.db.antlr4;
//}
//
//
///*
// * Parser Rules
// */
//ddlFile :
//	(createTable WHITESPACE?)*
//	EOF;
//	
//createTable  : 
//	CREATE WHITESPACE TABLE WHITESPACE 
//	tableNameSpace WHITESPACE 
//	content WHITESPACE
//	SEMI ;
//	
//tableNameSpace	: 
//	tableSegment 
//	(DOT tableSegment)*;
//tableSegment : LETTER*;
//
//content : LR_BRACKET fields RR_BRACKET;
//fields : (field COMMA)*? field;
//field : 
//	fieldName WHITESPACE 
//	fieldType WHITESPACE
//	COMMA WHITESPACE;
//fieldName :
//	LETTER+ (UNDERLINE+ LETTER+)*;
//fieldType :
//	LETTER+ fieldSize;
//fieldSize : LR_BRACKET NUMBER RR_BRACKET;
//
///*
// * Lexer Rules
// */
//fragment DEC_DIGIT   : [0-9];
//LETTER      : [a-zA-Z];
//UNDERLINE	: '_';
//DOT			: '.';
//CREATE 		: 'CREATE';
//TABLE 		: 'TABLE';
//WHITESPACE 	: [ \t]+ ;
//NEWLINE 	: [\r\n]+ ;
//NUMBER 		: [0-9]+ ;
//COMMA 		: ',';
//SEMI 		: ';';
//LR_BRACKET 	: '(';
//RR_BRACKET 	: ')';