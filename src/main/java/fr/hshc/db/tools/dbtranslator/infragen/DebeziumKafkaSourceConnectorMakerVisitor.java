package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.antlr4.DDLParser.CreateTableContext;
import fr.hshc.db.tools.dbcrawler.DatabaseConfig;

public class DebeziumKafkaSourceConnectorMakerVisitor extends SnowCodeGeneratorGenericVisitor {
	private DatabaseConfig databaseConfig;

	public DebeziumKafkaSourceConnectorMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String targetSchema, String dbConf) {
		super(typeMapping, workingDatabase, sourceSchema, null, targetSchema);
        databaseConfig = DatabaseConfig.loadAll(dbConf).getFirst();
	}

	public DebeziumKafkaSourceConnectorMakerVisitor(Map<String, String> typeMapping, String dbConf) {
		this(typeMapping,null,null,null,dbConf);
	}


//	cd $KAFKA_HOME
//	cat << EOF | tee config/dbz-sqlserver-source-connector.properties
//	name=dbz-sqlserver-connector
//	connector.class=io.debezium.connector.sqlserver.SqlServerConnector
//	database.hostname=$SQLSERVER_HOSTNAME
//	database.port=$SQLSERVER_PORT
//	database.user=$SQLSERVER_USER
//	database.password=$SQLSERVER_PWD
//	database.names=$SQLSERVER_DB_NAME
//	# root topic that holds DDL spec and changes
//	topic.prefix=dbzsqlserver
//	table.include.list=dbo.customers
//	# table.exclude.list=...
//	schema.history.internal.kafka.bootstrap.servers=$KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT
//	schema.history.internal.kafka.topic=schemahistory.dbzsqlserver
//	database.encrypt=false
//	EOF
	
	@Override
	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
		String namespace = databaseConfig.server+"_"+databaseConfig.database;
		String fileName = "dbz-"+databaseConfig.server+"_"+databaseConfig.database+"-source-connector";
		
		StringBuilder output = new StringBuilder("cd $KAFKA_HOME\r\n")
		.append(String.format("cat << EOF | tee config/%s.properties\r\n", fileName)+"\r\n")
		.append(String.format("name=%s\r\n", fileName)+"\r\n")
		.append("connector.class=io.debezium.connector.sqlserver.SqlServerConnector\r\n")
		.append("database.hostname=$SQLSERVER_HOSTNAME\r\n")
		.append("database.port=$SQLSERVER_PORT\r\n")
		.append("database.user=$SQLSERVER_USER\r\n")
		.append("database.password=$SQLSERVER_PWD\r\n")
		.append("database.names=$SQLSERVER_DB_NAME\r\n")
		.append("# root topic that holds DDL spec and changes\r\n")
		.append(String.format("topic.prefix=%s\r\n", namespace))
		.append(genTopics(ctx)+"\r\n")
		.append("# table.exclude.list=...\r\n")
		.append("schema.history.internal.kafka.bootstrap.servers=$KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT\r\n")
		.append(String.format("schema.history.internal.kafka.topic=schemahistory.%s\r\n", namespace))
		.append("database.encrypt=false\r\n")
		.append("EOF");
		// Visit all CREATE TABLE statements and join their transformations
		return output.toString();
	}
	
	
	public String genTopics(DDLParser.DdlFileContext ctx) {
		List<CreateTableContext> lst = ctx.createTable();
		StringBuilder result = new StringBuilder("table.include.list=");
		result.append(lst
					.stream()
					.map(this::genTopic)
					.collect(Collectors.joining(", ")));
		System.out.println(result);
		return result.toString();
	}
	
	public String genTopic(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();
		
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String outputFQTN = this.targetSchema + "." + this.sourceTableName;
	
		result.append(outputFQTN);
		return result.toString().toLowerCase();
	}
}
