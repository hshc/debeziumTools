package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public class SnowTargetTablesBootstrapingDMLMakerVisitor extends SnowsqlCodeGenVisitor {

	public SnowTargetTablesBootstrapingDMLMakerVisitor(Map<String, String> typeMapping, String landingSchema, String targetDatabase, String targetSchema) {
		super(typeMapping, null, null, landingSchema, targetDatabase, targetSchema);
	}

	public SnowTargetTablesBootstrapingDMLMakerVisitor(Map<String, String> typeMapping) {
		this(typeMapping,null,null,null);
	}

	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();

		// Extract table namespace and content
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String landingTableFQTN = "\t" + this.getLandingSchema() + "."+this.getSourceSchema().toUpperCase()+"_" + this.tableName + "\r\n";

		String fields = visitContent(ctx.content());

		String outputFQTN = this.getTargetSchema() + "." + this.tableName;

		result
		.append("INSERT INTO ").append(outputFQTN).append("\r\n")
		.append("    SELECT DISTINCT \r\n")
		.append(fields).append("\r\n")
		.append("    FROM\r\n")
		.append(landingTableFQTN).append("    WHERE\r\n")
		.append("        t.record_content:payload.op::text IN ('r');\r\n\r\n");
		return result.toString();
	}

	@Override
	public String visitField(DDLParser.FieldContext ctx) {
		Field field = super.extractField(ctx);
		if (field.id) {
			return String.format("COALESCE(t.record_content:payload.before.%s::%s, t.record_content:payload.after.%s::%s) as %s", field.name, field.type, field.name, field.type, field.name);
		} else if (field.size > 0) {
			return String.format("t.record_content:payload.before.%s::%s(%s) as %s", field.name, field.type.toLowerCase(), field.size, field.name);
		} else {
			return String.format("t.record_content:payload.before.%s::%s as %s", field.name, field.type.toLowerCase(), field.name);
		}
	}
}
