package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.tools.dbcrawler.DatabaseConfig;

public class SnowTargetTablesDDLMakerVisitor extends SnowCodeGeneratorGenericVisitor {
	private DatabaseConfig databaseConfig;

	public SnowTargetTablesDDLMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String targetSchema, String dbConf) {
		super(typeMapping, workingDatabase, sourceSchema, null, targetSchema);
        databaseConfig = DatabaseConfig.loadAll(dbConf).getFirst();
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
		initNameSpaces(ctx.tableNameSpace().getText());
		String hostAndDbNs = databaseConfig.server;
		if (!"".equals(this.getWorkingDatabase())) {
			hostAndDbNs += "_"+this.getWorkingDatabase();
		}
		
		StringBuilder result = new StringBuilder();

		String fields = visitContent(ctx.content());

		String outputFQTN = 
				hostAndDbNs + "." 
				+ this.getTargetSchema() + "." + this.tableName;

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
