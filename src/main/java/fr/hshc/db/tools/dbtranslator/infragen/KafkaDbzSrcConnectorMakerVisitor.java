package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.antlr4.DDLParser.CreateTableContext;

public class KafkaDbzSrcConnectorMakerVisitor extends KafkaConnectorMakerVisitor {
	public KafkaDbzSrcConnectorMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String targetSchema, String dbConf) {
		super(typeMapping, workingDatabase, sourceSchema, targetSchema, dbConf);
	}

	public KafkaDbzSrcConnectorMakerVisitor(Map<String, String> typeMapping, String dbConf) {
		super(typeMapping, dbConf);
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
	
//	@Override
//	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
//		String serverhost = databaseConfig.server;
//		String fileName = "dbz-"+databaseConfig.server+"_"+databaseConfig.database+"-source-connector";
//		
//		StringBuilder output = new StringBuilder("cd $KAFKA_HOME\r\n")
//		.append(String.format("cat << EOF | tee config/%s.properties\r\n", fileName))
//		.append("# ref: https://debezium.io/documentation/reference/stable/connectors/sqlserver.html#sqlserver-connector-properties\r\n\r\n")
//		.append(String.format("name=%s\r\n", fileName)+"\r\n")
//		.append("connector.class=io.debezium.connector.sqlserver.SqlServerConnector\r\n")
//		.append("database.hostname =$SQLSERVER_HOSTNAME\r\n")
//		.append("database.port     =$SQLSERVER_PORT\r\n")
//		.append("database.user     =$SQLSERVER_USER\r\n")
//		.append("database.password =$SQLSERVER_PWD\r\n")
//		.append("database.names    =$SQLSERVER_DB_NAME\r\n")
//		.append("\r\n")
//		.append("# root topic that holds DDL spec and changes\r\n")
//		.append(String.format("topic.prefix=%s\r\n", serverhost))
//		.append(genTopics(ctx)+"\r\n")
//		.append("\r\n")
//		.append("# table.exclude.list=...\r\n")
//		.append("schema.history.internal.kafka.bootstrap.servers=$KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT\r\n")
//		.append(String.format("schema.history.internal.kafka.topic=schemahistory.%s\r\n", serverhost))
//		.append("database.encrypt=false\r\n")
//		.append("EOF");
//		// Visit all CREATE TABLE statements and join their transformations
//		return output.toString();
//	}
//	
//	
//	public String genTopics(DDLParser.DdlFileContext ctx) {
//		List<CreateTableContext> lst = ctx.createTable();
//		StringBuilder result = new StringBuilder("table.include.list=");
//		result.append(lst
//					.stream()
//					.map(this::genTopic)
//					.collect(Collectors.joining(", ")));
//		System.out.println(result);
//		return result.toString();
//	}
	

	
	
	
	
	
	
	

	@Override
	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
		Map<String, List<CreateTableContext>> map = orderedFqtnByDbName(ctx);
		StringBuilder out = map.entrySet()
			.stream()
			.map(this::computeTopicsList)
			.map(this::computeKafkaSourceFileContent)
			.reduce(new StringBuilder("cd $KAFKA_HOME\r\n"), this::cumulateFiles);
		return out.toString();
	}

	private Entry<String, StringBuilder> computeTopicsList(Entry<String, List<CreateTableContext>> in) {
		StringBuilder result = new StringBuilder("table.include.list=");
		result.append(
			in.getValue().stream()
			.map(this::genTopic)
			.collect(Collectors.joining(", "))
		);
		Entry<String, StringBuilder> out = new AbstractMap.SimpleEntry<String, StringBuilder>(in.getKey(), result);
		return out;
	}
	
	private String genTopic(DDLParser.CreateTableContext ctx) {
		initNameSpaces(ctx.tableNameSpace().getText());
		String outputFQTN = this.getLandingSchema() + "." + this.tableName;
		return outputFQTN.toLowerCase();
	}
	
	private StringBuilder computeKafkaSourceFileContent(Entry<String, StringBuilder> in) {
		String serverhost = this.databaseConfig.server;
		String hostAndDbNs = this.databaseConfig.server+"_"+in.getKey();
		String fileName = "dbz-source-connector-"+hostAndDbNs;
		
		StringBuilder output = new StringBuilder(String.format("cat << EOF | tee config/%s.properties\r\n", fileName))
		.append("# ref: https://debezium.io/documentation/reference/stable/connectors/sqlserver.html#sqlserver-connector-properties\r\n\r\n")
		.append(String.format("name=%s\r\n", fileName)+"\r\n")
		.append("connector.class=io.debezium.connector.sqlserver.SqlServerConnector\r\n")
		.append("database.hostname =$SQLSERVER_HOSTNAME\r\n")
		.append("database.port     =$SQLSERVER_PORT\r\n")
		.append("database.user     =$SQLSERVER_USER\r\n")
		.append("database.password =$SQLSERVER_PWD\r\n")
		.append(String.format("database.names    =%s\r\n", in.getKey()))
		.append("\r\n")
		.append("# root topic that holds DDL spec and changes\r\n")
		.append(String.format("topic.prefix=%s\r\n", serverhost))
		.append(in.getValue()+"\r\n")
		.append("\r\n")
		.append("# table.exclude.list=...\r\n")
		.append("schema.history.internal.kafka.bootstrap.servers=$KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT\r\n")
		.append(String.format("schema.history.internal.kafka.topic=schemahistory.%s\r\n", serverhost))
		.append("database.encrypt=false\r\n")
		.append("EOF\r\n")
		.append(String.format("chmod 600 config/%s.properties", fileName));
		return output;
	}
	
	private StringBuilder cumulateFiles(StringBuilder a, StringBuilder b) {
		return a.append("\r\n\r\n").append(b);
	}
}
