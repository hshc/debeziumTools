parser grammar DDLParser;
@header {
package fr.hshc.db.antlr4;
}

options { tokenVocab=DDLLexer; }

ddlFile :
    (createTable WHITE_SPACE*)*
	EOF;
createTable  : 
	create table  tableNameSpace  
	content
	SEMI_COL ;
create :CREATE;
table :TABLE;

tableNameSpace	: 
	tableSegment 
	(DOT tableSegment)*
	WS*;

tableSegment : LETTERS (UNDERLINE+ LETTERS)*;


content : 
    LR_BRACKET 
    fields 
    RR_BRACKET WS*;

fields : (field COMMA)* field;

field : 
    fieldName WS*
    fieldType WS*
;

fieldName : LETTERS (UNDERLINE+ LETTERS)*;
fieldType : LETTERS (WS* filedIdent)? fieldSize?;
filedIdent : LETTERS;
fieldSize : LR_BRACKET NUMBERS RR_BRACKET;
