package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public abstract class SnowsqlCodeGenVisitor extends SnowCodeGenVisitor {

	public SnowsqlCodeGenVisitor(Map<String, String> typeMapping, String sourceDatabase, String sourceSchema, String landingSchema, String targetDatabase, String targetSchema) {
		super(typeMapping, sourceDatabase, sourceSchema, landingSchema, targetDatabase, targetSchema);
	}

	public SnowsqlCodeGenVisitor(Map<String, String> typeMapping) {
		super(typeMapping);
	}

	@Override
	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
		String result = super.visitDdlFile(ctx);
		result = "cat <<EOF | snowsql -o log_level=DEBUG -c $SNOW_PROFILE\r\n"
				+ "USE ROLE sysadmin;\r\n"
				+ "USE DATABASE "+ this.getTargetDatabase() + ";\r\n\r\n"
				+ result+"\r\n"
				+ "EOF";
		System.out.println(result);
		return result.toString();
	}

}