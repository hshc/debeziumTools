package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public class SnowTargetTablesSnapshotingDMLCTASMakerVisitor extends SnowsqlCodeGenVisitor {

	public SnowTargetTablesSnapshotingDMLCTASMakerVisitor(Map<String, String> typeMapping, String sourceSchema, String landingSchema, String targetDatabase, String targetSchema) {
		super(typeMapping, null, sourceSchema, landingSchema, targetDatabase, targetSchema);
	}

	public SnowTargetTablesSnapshotingDMLCTASMakerVisitor(Map<String, String> typeMapping) {
		this(typeMapping,null,null,null,null);
	}

	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();
		// Extract table namespace and content
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String outputFQTN = this.getTargetSchema() + "." + this.tableName;
		String inputFQTN = this.getLandingSchema() + "."+ this.getSourceSchema().toUpperCase()+"_" + this.tableName;

		String content = visitContent(ctx.content());
		content = String.format(content, inputFQTN, inputFQTN);
		result
		.append("insert into " + outputFQTN + "\r\n")
		.append(content).append("\r\n\r\n");

		return result.toString();
	}

	@Override
	public String visitFields(DDLParser.FieldsContext ctx) {
		// Build the list of fields
		StringBuilder result = new StringBuilder();

		StringBuilder fields_1 = new StringBuilder("");
		StringBuilder fields_2 = new StringBuilder("\t");
		StringBuilder fields_3 = new StringBuilder();
		StringBuilder idFields = new StringBuilder();
		for (int i = 0; i < ctx.field().size(); i++) {
			String field_1 = visitFieldForFinalSelect(ctx.field(i));
			String field_2 = visitFieldForFirstSelect(ctx.field(i));
			String field_3 = visitFieldForSecondSelect(ctx.field(i));
			String idField = visitFieldForFindingIdField(ctx.field(i));
			if (i > 0) {
				fields_1.append(", ");
				fields_2.append("\r\n\t, ");
			}
			fields_1.append(field_1);
			fields_2.append(field_2);
			fields_3.append(field_3);
			idFields.append(idField);
		}

		// SQLSERVER_INGEST.LANDING.DBO_CUSTOMERS
		result
		.append("select ").append(fields_1).append(" \r\n")
		.append("  from (\r\n").append("    select distinct\r\n")
//		.append("      --  COALESCE renvoie un premier argument non NULL à partir de la liste d'arguments passée\r\n")
//		.append("      -- for the 'd' case, the 'id' will only come through in the 'before' section\r\n")
		.append(fields_2).append("\r\n")
		.append("    from %s as t\r\n")
		.append("  ) as a\r\n")
		.append("  inner join (\r\n")
		.append("    -- Find the latest timestamp for each id\r\n")
		.append("    select ").append(fields_3).append("\r\n")
		.append("        , MAX(u.record_content:payload:source:ts_ms::integer) AS max_timestamp\r\n")
		.append("    from  %s as u\r\n")
		.append("    group by ").append(idFields).append("\r\n")
		.append("  ) as latest\r\n")
		.append("  on a.ts_ms = latest.max_timestamp and a.").append(idFields).append(" = latest.")
		.append(idFields).append(";");
		return result.toString();
	}

	public String visitFieldForFindingIdField(DDLParser.FieldContext ctx) {
		Field field = super.extractField(ctx);
		if (field.id) {
			return field.name;
		}
		return "";
	}

	public String visitFieldForFinalSelect(DDLParser.FieldContext ctx) {

		String fieldName = ctx.fieldName().getText();
		return fieldName;
	}

	public String visitFieldForFirstSelect(DDLParser.FieldContext ctx) {
		Field field = super.extractField(ctx);
		if (field.id) {
			return String.format("COALESCE(t.record_content:payload.before.%s::%s, t.record_content:payload.after.%s::%s) as %s", field.name, field.type.toLowerCase(), field.name, field.type.toLowerCase(), field.name);
		} else if (field.size > 0) {
			return String.format("t.record_content:payload.before.%s::%s(%s) as %s", field.name, field.type.toLowerCase(), field.size, field.name);
		} else {
			return String.format("t.record_content:payload.before.%s::%s as %s", field.name, field.type.toLowerCase(), field.name);
		}
	}

	public String visitFieldForSecondSelect(DDLParser.FieldContext ctx) {
		Field field = super.extractField(ctx);
		if (field.id) {
			return String.format("COALESCE(u.record_content:payload.before.%s::%s, u.record_content:payload.after.%s::%s) as %s", field.name, field.type.toLowerCase(), field.name, field.type.toLowerCase(), field.name);
		}
		return "";
	}
}
