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
	WS+;

tableSegment : (LETTER|NUMBER)+ (UNDERLINE+ (LETTER|NUMBER)+)*;


content : 
    LR_BRACKET WS+
    fields 
    RR_BRACKET WS*;

fields : (field comma)* field;

field : 
    fieldName WS+
	fieldType
	(WS+ filedIdent)? 
	(lr_bracket fieldSize rr_bracket)?
;

fieldName : (LETTER|NUMBER)+ (UNDERLINE+ (LETTER|NUMBER)+)*;
//fieldType : type (WS+ filedIdent)? fieldSize?;
fieldType : LETTER+;
filedIdent : LETTER+;
fieldSize : NUMBER+;

comma : WS* COMMA WS*;
lr_bracket : WS* LR_BRACKET WS*;
rr_bracket : WS* RR_BRACKET WS*;