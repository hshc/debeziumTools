package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.antlr4.DDLParser.CreateTableContext;
import fr.hshc.db.tools.dbcrawler.DatabaseConfig;

public class KafkaConnectorMakerVisitor extends SnowCodeGenVisitor {
	protected DatabaseConfig databaseConfig;

	public KafkaConnectorMakerVisitor(Map<String, String> typeMapping, String sourceDatabase, String sourceSchema, String targetDatabase, String targetSchema, String dbConf) {
		super(typeMapping, sourceDatabase, sourceSchema, null, targetDatabase, targetSchema);
        databaseConfig = DatabaseConfig.loadAll(dbConf).getFirst();
	}

	public KafkaConnectorMakerVisitor(Map<String, String> typeMapping, String dbConf) {
		this(typeMapping,null,null,null,null,dbConf);
	}
	
	protected Map<String, List<CreateTableContext>> orderedFqtnByDbName(DDLParser.DdlFileContext ctx) {
		List<CreateTableContext> lst = ctx.createTable();
		Map<String, List<CreateTableContext>> out = lst.stream()
		.collect(Collectors.groupingBy(this::databaseName));
		return out;
	}
	private String databaseName(CreateTableContext ctx) {
		initNameSpaces(ctx.tableNameSpace().getText().trim());
		return this.getSourceDatabase();
	}

}
