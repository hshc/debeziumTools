package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public class SnowBootstrapCTASMakerVisitor extends SnowCodeGenVisitor {

	public SnowBootstrapCTASMakerVisitor(Map<String, String> typeMapping, String sourceDatabase, String sourceSchema, String landingSchema, String targetDatabase, String targetSchema) {
		super(typeMapping, sourceDatabase, sourceSchema, landingSchema, targetDatabase, targetSchema);
	}

	public SnowBootstrapCTASMakerVisitor(Map<String, String> typeMapping) {
		this(typeMapping,null,null,null,null,null);
	}

	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();

		// Extract table namespace and content
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String landingTableFQTN = "\t";
		if (!"".equals(this.getSourceDatabase())) {
			landingTableFQTN = this.getSourceDatabase() + ".";
		}
		landingTableFQTN += this.getLandingSchema() + "."+this.getSourceSchema().toUpperCase()+"_" + this.tableName + "\r\n";

		String fields = visitContent(ctx.content());

		String outputFQTN = "";
		if (this.getSourceDatabase() != null) {
			outputFQTN = this.getSourceDatabase() + ".";
		}
		outputFQTN += this.getTargetSchema() + "." + this.tableName;

		result
		.append("CREATE OR REPLACE TABLE ").append(outputFQTN).append(" AS\r\n")
		.append("    SELECT DISTINCT \r\n")
		.append(fields).append("\r\n")
		.append("    FROM\r\n")
		.append(landingTableFQTN).append("    WHERE\r\n")
		.append("        t.record_content:payload.op::text IN ('r');\r\n\r\n");
		return result.toString();
	}

	@Override
	public String visitField(DDLParser.FieldContext ctx) {
		// Extract field name and format it
		String fieldName = visitFieldName(ctx.fieldName());
		Field fieldType = super.extractField(ctx);

		if (fieldType.id) {
			return String.format("COALESCE(t.record_content:payload.before.%s::%s, t.record_content:payload.after.%s::%s) as %s", fieldName, fieldType.type, fieldName, fieldType.type, fieldName);
		} else if (fieldType.size > 0) {
			return String.format("t.record_content:payload.before.%s::%s(%s) as %s", fieldName, fieldType.type.toLowerCase(), fieldType.size, fieldName);
		} else {
			return String.format("t.record_content:payload.before.%s::%s as %s", fieldName, fieldType.type.toLowerCase(), fieldName);
		}
	}
}
