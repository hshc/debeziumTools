# Configure Kafka 
## Server 

```sh
cp $KAFKA_HOME/config/server.properties $KAFKA_HOME/config/server.properties.bak
sed -i "s|log.dirs=.*|log.dirs=$KAFKA_SERVER_LOGS|g" $KAFKA_HOME/config/server.properties
```

### Exemple de Configuration Combinée

Pour créer un topic avec des configurations spécifiques de rétention et de taille, vous pouvez utiliser une commande combinée comme suit :

```sh
kafka-topics.sh --create --zookeeper <zookeeper_host> \
--replication-factor 3 --partitions 10 --topic <topic_name> \
--config retention.ms=172800000 --config retention.bytes=1073741824 \
--config segment.bytes=536870912 --config cleanup.policy=delete
```

Cette commande crée un topic avec :
- 10 partitions
- Réplication avec un facteur de 3
- Rétention des messages pendant 2 jours (172800000 ms)
- Taille maximale de rétention de 1 GB (1073741824 bytes)
- Taille de segment de 512 MB (536870912 bytes)
- Politique de nettoyage `delete`


## Install "Debezium SQLServer" Source Connector
### Download Connector
https://debezium.io/documentation/reference/stable/connectors/sqlserver.html#sqlserver-deploying-a-connector

https://repo1.maven.org/maven2/com/snowflake/snowflake-kafka-connector/2.1.2/snowflake-kafka-connector-2.1.2.jar

```sh
wget -O - https://repo1.maven.org/maven2/io/debezium/debezium-connector-sqlserver/$DBZ_SQLSERVER_SRC_CONNECTOR_VERSION/debezium-connector-sqlserver-$DBZ_SQLSERVER_SRC_CONNECTOR_VERSION-plugin.tar.gz \
| tar xz -C ~
find ~/debezium-connector-sqlserver -type f -name "*.jar" -exec mv {} 
$KAFKA_HOME/libs \;
rm -rf ~/debezium-connector-sqlserver
```

```sh
ls ./libs | grep debez
```

### Configure Connector

#### Add connector to classpath
https://kafka.apache.org/documentation/#connectconfigs_plugin.path

```sh
cp $KAFKA_HOME/config/connect-standalone.properties $KAFKA_HOME/config/dbz-connect-standalone.properties
sed -ri "/^plugin\.path=/ s|(.*)|&, libs/debezium-connector-sqlserver-$DBZ_SQLSERVER_SRC_CONNECTOR_VERSION.jar|" $KAFKA_HOME/config/dbz-connect-standalone.properties
diff $KAFKA_HOME/config/connect-standalone.properties $KAFKA_HOME/config/dbz-connect-standalone.properties
```

```sh
cat $KAFKA_HOME/config/dbz-connect-standalone.properties
```

#### Configure Debezium SQLServer source connector
https://debezium.io/documentation/reference/stable/connectors/sqlserver.html#sqlserver-example-configuration
https://learn.microsoft.com/fr-fr/sql/connect/jdbc/configuring-the-client-for-ssl-encryption?view=sql-server-ver16

##### By Cold ".properties" file
https://debezium.io/documentation/reference/stable/connectors/sqlserver.html#sqlserver-connector-properties
```toml
cd $KAFKA_HOME
cat << EOF | tee config/dbz-sqlserver-source-connector.properties
name=dbz-sqlserver-connector
connector.class=io.debezium.connector.sqlserver.SqlServerConnector
database.hostname=$SQLSERVER_HOSTNAME
database.port=$SQLSERVER_PORT
database.user=$SQLSERVER_USER
database.password=$SQLSERVER_PWD
database.names=$SQLSERVER_DB_NAME
# root topic that holds DDL spec and changes
topic.prefix=dbzsqlserver
table.include.list=dbo.customers
# table.exclude.list=...
schema.history.internal.kafka.bootstrap.servers=$KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT
schema.history.internal.kafka.topic=schemahistory.dbzsqlserver
database.encrypt=false
EOF
```

##### By Hot REST API call 

Ces confs permettent de parametrer à chaud les connecteurs kafka connect,
Cela revient au meme que dans la conf à froid, sauf que les documents de conf doivent être passés pas l'api Rest de KafkaConnect. 
L'interface Debezium UI s'appuye notament sur cette API pour déployer les connecteurs


```json
{
    "name": "dbz-sqlserver-connector", 
    "config": {
        "connector.class": "io.debezium.connector.sqlserver.SqlServerConnector", 
        "database.hostname": "127.0.0.1", 
        "database.port": "1433", 
        "database.user": "sa", 
        "database.password": "****", 
        "database.names": "debezium", 
        "topic.prefix": "dbzsqlserver", 
        "table.include.list": "dbo.customers", 
        "schema.history.internal.kafka.bootstrap.servers": "kafka:9092", 
        "schema.history.internal.kafka.topic": "schemahistory.dbzsqlserver" 
        "database.ssl.truststore": "path/to/trust-store" 
        "database.ssl.truststore.password": "password-for-trust-store" 
    }
}
```

```json
cat << EOF | tee register-dbz-sqlserver.json
{
    "name": "dbz-sqlserver-connector", 
    "config": {
        "connector.class": "io.debezium.connector.sqlserver.SqlServerConnector", 
        "database.hostname": "$SQLSERVER_HOSTNAME", 
        "database.port": "$SQLSERVER_PORT", 
        "database.user": "$SQLSERVER_USER", 
        "database.password": "$SQLSERVER_PWD", 
        "database.names": "$SQLSERVER_DB_NAME", 
        "topic.prefix": "dbzsqlserver", 
        "table.include.list": "dbo.customers", 
        "schema.history.internal.kafka.bootstrap.servers": "$KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT", 
        "schema.history.internal.kafka.topic": "schemahistory.dbzsqlserver",
        "database.encrypt": "false"
    }
}
EOF
```

##### Test
Launch following to see then stop
```sh
cd $KAFKA_HOME
./bin/connect-standalone.sh \
	config/dbz-connect-standalone.properties \
	config/dbz-sqlserver-source-connector.properties | tee $KAFKA_OUTPUT_TRACES/connectOutput.log &
```

KO KO KO
```sh
keytool -printcert -rfc -sslserver localhost:1434 -v
```
KO KO KO
```sh
keytool -import -v -trustcacerts -alias myServer -file caCert.cer -keystore truststore.ks
```



> [!tip] note
> It’s important to monitor database metrics so that you know if the database reaches the point where the server can no longer support the capture agent’s level of activity. If you notice performance problems, there are SQL Server capture agent settings that you can modify to help balance the overall CPU load on the database host with a tolerable degree of latency.

## Install "Snowflake" Sink Connector
https://www.pythian.com/blog/replicating-mysql-to-snowflake-with-kafka-and-debezium-part-one-data-extraction
https://www.pythian.com/blog/replicating-mysql-to-snowflake-with-kafka-and-debezium-part-two-data-ingestion
https://docs.snowflake.com/en/user-guide/kafka-connector-install
https://docs.snowflake.com/en/user-guide/kafka-connector-install#download-the-kafka-connector-jar-files
https://docs.snowflake.com/en/user-guide/kafka-connector

```ad-hint
Snowflake recommends that you create a separate user (using [CREATE USER](https://docs.snowflake.com/en/sql-reference/sql/create-user)) and role (using [CREATE ROLE](https://docs.snowflake.com/en/sql-reference/sql/create-role)) for each Kafka instance so that the access privileges can be individually revoked if needed. The role should be assigned as the default role for the user.
```

```ad-warning
title: Important

The Kafka Connect framework broadcasts the configuration settings for the Kafka connector from the master node to worker nodes. The configuration settings include sensitive information (specifically, the Snowflake username and private key). Make sure to secure the communication channel between Kafka Connect nodes. For instructions, see the documentation for your Apache Kafka software.
```
```ad-warning
title: Important
Because the configuration file typically contains security related information, such as the private key, set read/write privileges appropriately on the file to limit access.
In addition, consider storing the configuration file in a secure external location or a key management service. For more information, see [Externalizing Secrets](https://docs.snowflake.com/en/user-guide/kafka-connector-install#externalizing-secrets) (in this topic).
```

### Download  Connector
https://mvnrepository.com/artifact/com.snowflake
```sh
cd $KAFKA_HOME/libs
wget https://repo1.maven.org/maven2/com/snowflake/snowflake-kafka-connector/$SNOW_SINK_CONNECTOR_VERSION/snowflake-kafka-connector-$SNOW_SINK_CONNECTOR_VERSION.jar
wget https://repo1.maven.org/maven2/org/bouncycastle/bc-fips/1.0.1/bc-fips-1.0.1.jar
wget https://repo1.maven.org/maven2/org/bouncycastle/bcpkix-fips/1.0.3/bcpkix-fips-1.0.3.jar
```

```sh
ls ./libs | grep snow
```


### Configure Connector 

#### Add connector to classpath

https://kafka.apache.org/documentation/#connectconfigs_plugin.path

```sh
cp $KAFKA_HOME/config/dbz-connect-standalone.properties $KAFKA_HOME/config/dbz-connect-standalone.properties.bak
sed -ri "/^plugin\.path=/ s|(.*)|&, libs/snowflake-kafka-connector-$SNOW_SINK_CONNECTOR_VERSION.jar, libs/bc-fips-1.0.1.jar, libs/bcpkix-fips-1.0.3.jar|" $KAFKA_HOME/config/dbz-connect-standalone.properties
diff $KAFKA_HOME/config/dbz-connect-standalone.properties $KAFKA_HOME/config/dbz-connect-standalone.properties.bak
```

```sh
cat $KAFKA_HOME/config/dbz-connect-standalone.properties
```

#### Configure Snowflake sink connector
https://docs.snowflake.com/en/user-guide/kafka-connector-install#label-kafka-properties
https://docs.snowflake.com/en/user-guide/kafka-connector-overview
##### By Cold ".properties" file

```toml
cd $SNOW_SCRIPTS
export SNOW_PRIV_KEY=$(grep -v PRIVATE snow_rsa_key.p8 | sed ':a;N;$!ba;s/\n//g')
export SNOW_PUB_KEY=$(grep -v PUBLIC snow_rsa_key.pub | sed ':a;N;$!ba;s/\n//g')
cd $KAFKA_HOME
cat << EOF | tee config/dbz-snowflake-sink-connector.properties
name=dbz-snowflake-sink-connector
connector.class=com.snowflake.kafka.connector.SnowflakeSinkConnector
tasks.max=2
topics=dbzsqlserver.$SQLSERVER_DB_NAME.dbo.customers
snowflake.topic2table.map=dbzsqlserver.$SQLSERVER_DB_NAME.dbo.customers:dbo_customers
buffer.count.records=10000
buffer.flush.time=60
buffer.size.bytes=5000000
snowflake.url.name=https://$SNOW_ACCOUNT.snowflakecomputing.com
snowflake.user.name=$SNOW_LANDING_USER
snowflake.private.key=$SNOW_PRIV_KEY
snowflake.private.key.passphrase=$SNOW_P8_FILE_PWD
snowflake.database.name=sqlserver_ingest
snowflake.schema.name=landing
key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=com.snowflake.kafka.connector.records.SnowflakeJsonConverter
EOF
chmod 600 config/dbz-snowflake-sink-connector.properties
```



https://docs.snowflake.com/en/user-guide/kafka-connector-install#label-kafka-properties
```json
{
  "name":"XYZCompanySensorData",
  "config":{
    "connector.class":"com.snowflake.kafka.connector.SnowflakeSinkConnector",
    "tasks.max":"8",
    "topics":"topic1,topic2",
    "snowflake.topic2table.map": "topic1:table1,topic2:table2",
    "buffer.count.records":"10000",
    "buffer.flush.time":"60",
    "buffer.size.bytes":"5000000",
    "snowflake.url.name":"myorganization-myaccount.snowflakecomputing.com:443",
    "snowflake.user.name":"jane.smith",
    "snowflake.private.key":"xyz123",
    "snowflake.private.key.passphrase":"jkladu098jfd089adsq4r",
    "snowflake.database.name":"mydb",
    "snowflake.schema.name":"myschema",
    "key.converter":"org.apache.kafka.connect.storage.StringConverter",
    "value.converter":"com.snowflake.kafka.connector.records.SnowflakeAvroConverter",
    "value.converter.schema.registry.url":"http://localhost:8081",
    "value.converter.basic.auth.credentials.source":"USER_INFO",
    "value.converter.basic.auth.user.info":"jane.smith:MyStrongPassword"
  }
}
```

## Install Y branch file Sink Connector

```toml
cd $KAFKA_HOME
cat << EOF | tee config/dbz-file-sink-connector.properties
name=dbz-file-sink-connector
connector.class=FileStreamSink
tasks.max=1
file=test.sink.txt
topics=dbzsqlserver.$SQLSERVER_DB_NAME.dbo.customers
EOF
chmod 600 config/dbz-file-sink-connector.properties
```