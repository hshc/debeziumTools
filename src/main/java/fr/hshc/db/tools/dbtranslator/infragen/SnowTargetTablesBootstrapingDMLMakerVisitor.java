package fr.hshc.db.tools.dbtranslator.infragen;

import java.io.IOException;
import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public class SnowTargetTablesBootstrapingDMLMakerVisitor extends SnowCodeGeneratorGenericVisitor {

	public SnowTargetTablesBootstrapingDMLMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String landingSchema, String targetSchema) {
		super(typeMapping, workingDatabase, sourceSchema, landingSchema, targetSchema);
	}

	public SnowTargetTablesBootstrapingDMLMakerVisitor(Map<String, String> typeMapping) {
		this(typeMapping,null,null,null,null);
	}

	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();

		// Extract table namespace and content
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String landingTableFQTN = "\t";
		if (!"".equals(this.workingDatabase)) {
			landingTableFQTN = this.workingDatabase + ".";
		}
		landingTableFQTN += this.landingSchema + "."+this.sourceSchema.toUpperCase()+"_" + this.sourceTableName + "\r\n";

		String fields = visitContent(ctx.content());

		String outputFQTN = "";
		if (!"".equals(this.workingDatabase)) {
			landingTableFQTN = this.workingDatabase + ".";
		}
		outputFQTN += this.targetSchema + "." + this.sourceTableName;

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
		// Extract field name and format it
		String fieldName = visitFieldName(ctx.fieldName());
		Field fieldType = null;
		try {
			fieldType = Field.deserialize(visitFieldType(ctx.fieldType()));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (fieldType.id) {
			return String.format("COALESCE(t.record_content:payload.before.%s::%s, t.record_content:payload.after.%s::%s) as %s", fieldName, fieldType.type, fieldName, fieldType.type, fieldName);
		} else if (fieldType.size > 0) {
			return String.format("t.record_content:payload.before.%s::%s(%s) as %s", fieldName, fieldType.type.toLowerCase(), fieldType.size, fieldName);
		} else {
			return String.format("t.record_content:payload.before.%s::%s as %s", fieldName, fieldType.type.toLowerCase(), fieldName);
		}
	}
}
