package fr.hshc.db.tools.dbtranslator.infragen;

import java.io.IOException;
import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public class SnowBronzeTablesMakerVisitor extends SnowCodeGeneratorGenericVisitor {

	public SnowBronzeTablesMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String targetSchema) {
		super(typeMapping, workingDatabase, sourceSchema, null, targetSchema);
	}

	public SnowBronzeTablesMakerVisitor(Map<String, String> typeMapping) {
		this(typeMapping,null,null,null);
	}

	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();

		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String fields = visitContent(ctx.content());

		String outputFQTN = "";
		if (!"".equals(this.workingDatabase)) {
			outputFQTN = this.workingDatabase + ".";
		}
		outputFQTN += this.targetSchema + "." + this.sourceTableName;

		result
		.append("CREATE TABLE ").append(outputFQTN).append(" (\r\n")
		.append(fields).append("\r\n);\r\n\r\n");
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

		if (fieldType.size > 0) {
			return String.format("%s %s(%s)", fieldName, fieldType.type.toLowerCase(), fieldType.size);
		}
		return String.format("%s %s", fieldName, fieldType.type.toLowerCase());
	}
}
