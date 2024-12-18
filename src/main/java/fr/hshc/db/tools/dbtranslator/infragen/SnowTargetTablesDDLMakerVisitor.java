package fr.hshc.db.tools.dbtranslator.infragen;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.antlr4.DDLParser.CreateTableContext;
import fr.hshc.db.tools.dbcrawler.DatabaseConfig;

public class SnowTargetTablesDDLMakerVisitor extends SnowCodeGeneratorGenericVisitor {
	private DatabaseConfig databaseConfig;

	public SnowTargetTablesDDLMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String targetSchema, String dbConf) {
		super(typeMapping, workingDatabase, sourceSchema, null, targetSchema);
        databaseConfig = DatabaseConfig.loadAll(dbConf).getFirst();
		String hostAndDbNs = databaseConfig.server+"_"+databaseConfig.database;
		this.workingDatabase = hostAndDbNs;
	}

	public SnowTargetTablesDDLMakerVisitor(Map<String, String> typeMapping, String dbConf) {
		this(typeMapping,null,null,null,dbConf);
	}


	@Override
	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
		String result = super.visitDdlFile(ctx);
		result = "cat <<EOF | snowsql -o log_level=DEBUG -c example\r\n"
				+ "use role sysadmin;\r\n"
				+ result+"\r\n"
				+ "EOF";
		System.out.println(result);
		return result.toString();
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
		.append("create or replace table ").append(outputFQTN).append(" (\r\n")
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
