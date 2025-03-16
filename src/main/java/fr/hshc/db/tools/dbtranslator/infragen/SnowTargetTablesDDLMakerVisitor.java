package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public class SnowTargetTablesDDLMakerVisitor extends SnowsqlCodeGenVisitor {

	public SnowTargetTablesDDLMakerVisitor(Map<String, String> typeMapping, String sourceDatabase, String sourceSchema, String targetDatabase, String targetSchema, String dbConf) {
		super(typeMapping, sourceDatabase, sourceSchema, null, targetDatabase, targetSchema);
	}

	public SnowTargetTablesDDLMakerVisitor(Map<String, String> typeMapping, String dbConf) {
		this(typeMapping,null,null,null,null,dbConf);
	}


	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		initNameSpaces(ctx.tableNameSpace().getText());
		
		String outputFQTN = this.getTargetSchema() + "." + this.tableName;
		
		StringBuilder result = new StringBuilder();

		String fields = visitContent(ctx.content());

		result
		.append("create or replace table ").append(outputFQTN).append(" (\r\n")
		.append(fields).append("\r\n);\r\n\r\n");
		return result.toString();
	}

	@Override
	public String visitField(DDLParser.FieldContext ctx) {
		Field field = super.extractField(ctx);
		if (field.size > 0) {
			return String.format("%s %s(%s)", field.name, field.type.toLowerCase(), field.size);
		}
		return String.format("%s %s", field.name, field.type.toLowerCase());
	}
}
