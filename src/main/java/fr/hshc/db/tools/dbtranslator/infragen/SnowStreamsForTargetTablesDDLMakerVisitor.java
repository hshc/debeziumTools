package fr.hshc.db.tools.dbtranslator.infragen;

import fr.hshc.db.antlr4.DDLParser;

public class SnowStreamsForTargetTablesDDLMakerVisitor extends SnowCodeGeneratorGenericVisitor {


	public SnowStreamsForTargetTablesDDLMakerVisitor(String workingDatabase, String sourceSchema, String landingSchema) {
		super(null, workingDatabase, sourceSchema, landingSchema, null);
	}

	public SnowStreamsForTargetTablesDDLMakerVisitor() {
		this(null,null,null);
	}


	@Override
	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
		String result = super.visitDdlFile(ctx);
		
		if (!"".equals(this.workingDatabase)) {
			result = "USE "+this.workingDatabase + ";\r\n\r\n" +result;
		}
		return result;
	}

	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();
		// Extract table namespace and content
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String streamFullNS = this.landingSchema + ".DBZ_"+this.sourceSchema.toUpperCase()+"_" + this.sourceTableName + "_STRM";
		String landingTableNS = this.landingSchema + "."+this.sourceSchema.toUpperCase()+"_" + this.sourceTableName;

		result
		.append("CREATE OR REPLACE STREAM ").append(streamFullNS).append(" \r\n")
		.append("ON TABLE ").append(landingTableNS).append(" \r\n")
		.append("APPEND_ONLY = true;\r\n\r\n");

		return result.toString();
	}
}