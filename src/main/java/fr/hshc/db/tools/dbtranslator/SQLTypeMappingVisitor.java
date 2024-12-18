package fr.hshc.db.tools.dbtranslator;

import java.util.Map;

import org.antlr.v4.runtime.tree.TerminalNode;

import fr.hshc.db.antlr4.DDLParser.ContentContext;
import fr.hshc.db.antlr4.DDLParser.FieldContext;
import fr.hshc.db.antlr4.DDLParser.FieldNameContext;
import fr.hshc.db.antlr4.DDLParser.FieldSizeContext;
import fr.hshc.db.antlr4.DDLParser.FieldsContext;
import fr.hshc.db.antlr4.DDLParser.FiledIdentContext;
import fr.hshc.db.antlr4.DDLParser.TableNameSpaceContext;
import fr.hshc.db.antlr4.DDLParser.TableSegmentContext;
import fr.hshc.db.antlr4.DDLParserBaseVisitor;

public class SQLTypeMappingVisitor extends DDLParserBaseVisitor<String> {
    private final Map<String, String> typeMapping;

    public SQLTypeMappingVisitor(Map<String, String> typeMapping) {
        this.typeMapping = typeMapping;
    }


	@Override
	public String visitFieldName(FieldNameContext ctx) {
		return ctx.getText();
	}

    @Override
    public String visitField(FieldContext ctx) {
    	String dbms1Type = ctx.children.getFirst().toString().toUpperCase();
        String dbms2Type = null;
    	FiledIdentContext ident = null;
    	FieldSizeContext size = null;
    	int i = ctx.getChildCount();
    	if (i > 1) {
    		ident = ctx.filedIdent();
    		size = ctx.fieldSize();
    	}
    	
        if (typeMapping.containsKey(dbms1Type)) {
        	dbms2Type = typeMapping.get(dbms1Type);
        }
        if (dbms2Type == null) {
        	dbms2Type = dbms1Type;
        }
        if (ident != null) {
        	dbms2Type = dbms2Type + " " + ident.getText();
        }
        if (size != null) {
        	dbms2Type = dbms2Type + size.getText();
        }
        return dbms2Type;
    }


	@Override
	public String visitFieldSize(FieldSizeContext ctx) {
		return ctx.getText();
	}


	@Override
	public String visitTableNameSpace(TableNameSpaceContext ctx) {
		int i = ctx.getChildCount();
		String out ="";
		for (int j = 0;j < i+1; j=j+2) {
			out += ctx.getChild(j).getText();
			if (j+2 < i+1)
				out += ".";
		}
		return out;
	}

	

	@Override
	public String visitContent(ContentContext ctx) {
		String out = "";
		out += ctx.children.getFirst().toString() + "\r\n";
		FieldsContext fieldsCtxt = ctx.fields();
		out += fieldsCtxt.accept(this)+ "\r\n";
		out += ctx.children.getLast().toString();
		return out;
	}


	@Override
	public String visitTableSegment(TableSegmentContext ctx) {
		return ctx.getText();
	}

	



	@Override
	public String visitTerminal(TerminalNode node) {
		return node.getText();
	}


	@Override
	protected String defaultResult() {
		return "";
	}


	@Override
	protected String aggregateResult(String aggregate, String nextResult) {
		return aggregate + " " + nextResult;
	}

//    @Override
//    public String visitChildren(RuleNode node) {
//        StringBuilder result = new StringBuilder();
//        for (int i = 0; i < node.getChildCount(); i++) {
//            result.append(node.getChild(i).accept(this));
//        }
////        if (0 == node.getChildCount()) {
////        	System.out.println("termination node : " + node.getText());
////        }
////        System.out.println("termination node : " + node.getText());
////        result.append(" ").append(node.getText());
////        System.out.println(result);
////        return result.toString();
//        if (node.getText() != null)
//        	result.append(node.getText());
//        System.out.println(result);
//        return result.toString();
//    }
    
}
