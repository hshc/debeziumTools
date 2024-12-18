package fr.hshc.db.tools.dbtranslator.infragen;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.antlr4.DDLParser.CreateTableContext;
import fr.hshc.db.tools.dbcrawler.DatabaseConfig;

public class DbzKafkaSnowSinkConnectorMakerVisitor extends SnowCodeGeneratorGenericVisitor {
	private DatabaseConfig databaseConfig;

	public DbzKafkaSnowSinkConnectorMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String targetSchema, String dbConf) {
		super(typeMapping, workingDatabase, sourceSchema, null, targetSchema);
        databaseConfig = DatabaseConfig.loadAll(dbConf).getFirst();
	}

	public DbzKafkaSnowSinkConnectorMakerVisitor(Map<String, String> typeMapping, String dbConf) {
		this(typeMapping,null,null,null,dbConf);
	}


	//topics=dbzdmh.$SQLSERVERDMH_DB_NAME.dbo.customers
	//snowflake.topic2table.map=dbzdmh.$SQLSERVERDMH_DB_NAME.dbo.customers:dbo_customers
	
//	cd $KAFKA_HOME
//	cat << EOF | tee config/dbz-snowflake-sink-connector.properties
//	name=dbz-snowflake-sink-connector
//	connector.class=com.snowflake.kafka.connector.SnowflakeSinkConnector
//	tasks.max=2
//	topics=dbzsqlserver.$SQLSERVER_DB_NAME.dbo.customers
//	snowflake.topic2table.map=dbzsqlserver.$SQLSERVER_DB_NAME.dbo.customers:dbo_customers
//	buffer.count.records=10000
//	buffer.flush.time=60
//	buffer.size.bytes=5000000
//	snowflake.url.name=https://$SNOW_ACCOUNT.snowflakecomputing.com
//	snowflake.user.name=$SNOW_LANDING_USER
//	snowflake.private.key=$SNOW_PRIV_KEY
//	snowflake.private.key.passphrase=$SNOW_P8_FILE_PWD
//	snowflake.database.name=sqlserver_ingest
//	snowflake.schema.name=landing
//	key.converter=org.apache.kafka.connect.storage.StringConverter
//	value.converter=com.snowflake.kafka.connector.records.SnowflakeJsonConverter
//	EOF
//	chmod 600 config/dbz-snowflake-sink-connector.properties
	@Override
	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
		String hostAndDbNs = databaseConfig.server+"_"+databaseConfig.database;
		String fileName = "dbz-snow"+databaseConfig.server+"_"+databaseConfig.database+"-sink-connector";
		
		StringBuilder output = new StringBuilder("cd $KAFKA_HOME\r\n")
		.append(String.format("cat << EOF | tee config/%s.properties\r\n", fileName)+"\r\n")
		.append(String.format("name=%s\r\n", fileName)+"\r\n")
		.append("connector.class=com.snowflake.kafka.connector.SnowflakeSinkConnector\r\n")
		.append("tasks.max=2\r\n")
		.append(genTopics(ctx)+"\r\n")
		.append(genTopic2tableMaps(ctx)+"\r\n")
		.append("buffer.count.records=10000\r\n")
		.append("buffer.flush.time=60\r\n")
		.append("buffer.size.bytes=5000000\r\n")
		.append("snowflake.url.name=https://$SNOW_ACCOUNT.snowflakecomputing.com\r\n")
		.append("snowflake.user.name=$SNOW_LANDING_USER\r\n")
		.append("snowflake.private.key=$SNOW_PRIV_KEY\r\n")
		.append("snowflake.private.key.passphrase=$SNOW_P8_FILE_PWD\r\n")
		.append(String.format("snowflake.database.name=%s\r\n", hostAndDbNs))
		.append("snowflake.schema.name=landing\r\n")
		.append("key.converter=org.apache.kafka.connect.storage.StringConverter\r\n")
		.append("value.converter=com.snowflake.kafka.connector.records.SnowflakeJsonConverter\r\n")
		.append("EOF\r\n")
		.append(String.format("chmod 600 config/%s.properties", fileName));
		// Visit all CREATE TABLE statements and join their transformations
		return output.toString();
	}
	
	
	public String genTopics(DDLParser.DdlFileContext ctx) {
		List<CreateTableContext> lst = ctx.createTable();
		StringBuilder result = new StringBuilder("topics=");
		result.append(lst
					.stream()
					.map(this::genTopic)
					.collect(Collectors.joining(", ")));
		System.out.println(result);
		return result.toString();
	}
	
	public String genTopic2tableMaps(DDLParser.DdlFileContext ctx) {
		List<CreateTableContext> lst = ctx.createTable();
		StringBuilder result = new StringBuilder("snowflake.topic2table.map=");
		result.append(lst
					.stream()
					.map(this::genTopic2tableMap)
					.collect(Collectors.joining(", ")));
		System.out.println(result);
		return result.toString();
	}
	
	public String genTopic(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();
		
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String outputFQTN = databaseConfig.server+ ".";
		if (!"".equals(this.workingDatabase)) {
			outputFQTN += this.workingDatabase;
		} else {
			outputFQTN += databaseConfig.database;
		}
		outputFQTN += "." + this.targetSchema + "." + this.sourceTableName;
	
		result.append(outputFQTN);
		return result.toString().toLowerCase();
	}
	
	public String genTopic2tableMap(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();
		
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String outputFQTN = databaseConfig.server+ ".";
		if (!"".equals(this.workingDatabase)) {
			outputFQTN += this.workingDatabase;
		} else {
			outputFQTN += databaseConfig.database;
		}
		outputFQTN += "." + this.targetSchema + "." + this.sourceTableName;
	
		result.append(outputFQTN.toLowerCase()).append(":").append(this.sourceSchema.toUpperCase()).append("_").append(this.sourceTableName.toUpperCase());
		return result.toString();
	}
}
