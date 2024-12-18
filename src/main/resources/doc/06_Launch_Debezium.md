# Launch Debezium
## full kafka reset

```
rm -rf $KAFKA_SERVER_LOGS
rm $KAFKA_OUTPUT_TRACES/*
mkdir -p $KAFKA_OUTPUT_TRACES
ll $KAFKA_OUTPUT_TRACES
rm $KAFKA_HOME/logs/*
ll  $KAFKA_HOME/logs
```
## all in one Kafka Start

```
rm $KAFKA_OUTPUT_TRACES/*
ll $KAFKA_OUTPUT_TRACES
rm $KAFKA_HOME/logs/*
ll  $KAFKA_HOME/logs

cd $KAFKA_HOME
nohup ./bin/zookeeper-server-start.sh 	config/zookeeper.properties > $KAFKA_OUTPUT_TRACES/zookeeper_`date "+%F_%H-%M"`.log  2>&1 &
nohup ./bin/kafka-server-start.sh 	config/server.properties > $KAFKA_OUTPUT_TRACES/kafkaServer_`date "+%F_%H-%M"`.log 2>&1 &
nohup ./bin/connect-standalone.sh \
	config/dbz-connect-standalone.properties \
	config/dbz-sqlserver-source-connector.properties \
	config/dbz-file-sink-connector.properties \
	config/dbz-snowflake-sink-connector.properties > $KAFKA_OUTPUT_TRACES/connect_`date "+%F_%H-%M"`.log 2>&1 &
# tail -100f $KAFKA_OUTPUT_TRACES/zookeeper_*.log &
# tail -100f $KAFKA_OUTPUT_TRACES/kafkaServer_*.log &
tail -100f $KAFKA_OUTPUT_TRACES/connect_*.log
```

## Start zookeeper, kafka and KafkaConnect

```
rm $KAFKA_OUTPUT_TRACES/*
mkdir -p $KAFKA_OUTPUT_TRACES
rm $KAFKA_HOME/logs/*
```

### Zookeeper launch
```sh
cd $KAFKA_HOME
nohup ./bin/zookeeper-server-start.sh 	config/zookeeper.properties > $KAFKA_OUTPUT_TRACES/zookeeper_`date "+%F_%H-%M"`.log  2>&1 &
```
OR
```sh
cd $KAFKA_HOME
./bin/zookeeper-server-start.sh 	config/zookeeper.properties > $KAFKA_OUTPUT_TRACES/zookeeper_`date "+%F_%H-%M"`.log
```


```sh
tail -100f $KAFKA_OUTPUT_TRACES/zookeeper_*.log
```

### Kafka Server launch
```sh
cd $KAFKA_HOME
nohup ./bin/kafka-server-start.sh 	config/server.properties > $KAFKA_OUTPUT_TRACES/kafkaServer_`date "+%F_%H-%M"`.log 2>&1 &
```
OR
```sh
cd $KAFKA_HOME
./bin/kafka-server-start.sh 	config/server.properties > $KAFKA_OUTPUT_TRACES/kafkaServer_`date "+%F_%H-%M"`.log
```


```sh
tail -100f $KAFKA_OUTPUT_TRACES/kafkaServer_*.log
```

### Kafka Connect launch
#### For SQLServer checks

```sh
cd $KAFKA_HOME
./bin/connect-standalone.sh \
	config/dbz-connect-standalone.properties \
	config/dbz-sqlserver-source-connector.properties | tee $KAFKA_OUTPUT_TRACES/connectOutput.log &
```


```sh
cd $KAFKA_HOME
./bin/connect-standalone.sh \
	config/dbz-connect-standalone.properties \
	config/dbz-vwc2bdd052_e2i5_dmh_dwh-source-connector.properties | tee $KAFKA_OUTPUT_TRACES/connectOutput.log &
```

#### From kafka connect tuto

```sh
cd $KAFKA_HOME
nohup ./bin/connect-standalone.sh \
	config/dbz-connect-standalone.properties \
	config/dbz-sqlserver-source-connector.properties \
	config/dbz-snowflake-sink-connector.properties > $KAFKA_OUTPUT_TRACES/connect_`date "+%F_%H-%M"`.log 2>&1 &
```
OR
```sh
cd $KAFKA_HOME
./bin/connect-standalone.sh \
	config/dbz-connect-standalone.properties \
	config/dbz-sqlserver-source-connector.properties \
	config/dbz-snowflake-sink-connector.properties > $KAFKA_OUTPUT_TRACES/connect_`date "+%F_%H-%M"`.log
```

```sh
tail -100f $KAFKA_OUTPUT_TRACES/connect_*.log
```

#### exemple
3 fichiers de paramétrages :
- le premier pour la conf de connexion au broker kafka 
- les 2 suivants pour le connecteur d'entrée (source) et de sortie (sink)
```sh
# start two connectors running in _standalone_ mode, which means they run in a single, local, dedicated process
./bin/connect-standalone.sh \
	config/connect-standalone.properties \
	config/connect-file-source.properties \
	config/connect-file-sink.properties &
```

### Troubleshooting
#### Debug Mode 
```sh
# to do before launching kafka connect
export EXTRA_ARGS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
./bin/connect-standalone.sh \
	config/dbz-connect-standalone.properties \
	| tee $KAFKA_OUTPUT_TRACES/connectOutput.log &
```

#### Logs
Eventually configure KafkaConnect log4j : 

```sh
vi $KAFKA_HOME/config/connect-log4j.properties
```
log4j.rootLogger=TRACE, stdout, connectAppender

## Checks 

```sh
curl -i -H "Accept:application/json" $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/
```

```sh
curl -i -H "Accept:application/json" $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/
```








## Debezium-UI


```sh
# "--add-host" allows container to access kafka runing on the host through the "host-gateway"
# https://docs.docker.com/engine/reference/commandline/exec/
sudo docker run --add-host=connect:host-gateway -it --rm --name debezium-ui -p 8080:8080 -e KAFKA_CONNECT_URIS=http://connect:8083 quay.io/debezium/debezium-ui 
```

```sh
# connect inside the container
sudo docker exec -it debezium-ui sh
```


```sh
find ./ -name '*.jar' -exec grep -iHsl DataCollectionId.class {} \;
./debezium-api-2.4.1.Final.jar
```
                                                       io.debezium.converters.spi.CloudEventsProvider

```sh
find ./ -name '*.jar' -exec grep -iHsl io/debezium/converters/spi/CloudEventsProvider.class {} \;
./debezium-core-2.4.1.Final.jar
```

                                                       io.debezium.connector.sqlserver.converters.SqlServerCloudEventsProvider
```sh
find ./ -name '*.jar' -exec grep -iHsl io/debezium/connector/sqlserver/converters/SqlServerCloudEventsProvider.class {} \;
./debezium-connector-sqlserver-2.4.1.Final.jar
```

```ad-error
ERROR Stopping due to error (org.apache.kafka.connect.cli.AbstractConnectCli:100)
java.util.ServiceConfigurationError: io.debezium.converters.spi.CloudEventsProvider: io.debezium.connector.sqlserver.converters.SqlServerCloudEventsProvider not a subtype
        at java.base/java.util.ServiceLoader.fail(ServiceLoader.java:589)
        at java.base/java.util.ServiceLoader$LazyClassPathLookupIterator.hasNextService(ServiceLoader.java:1237)
        at java.base/java.util.ServiceLoader$LazyClassPathLookupIterator.hasNext(ServiceLoader.java:1265)
        at java.base/java.util.ServiceLoader$2.hasNext(ServiceLoader.java:1300)
        at java.base/java.util.ServiceLoader$3.hasNext(ServiceLoader.java:1385)
        at io.debezium.converters.CloudEventsConverter.<clinit>(CloudEventsConverter.java:131)
```


```sh
find ./ -name '*.jar' -exec grep -iHsl \
io/debezium/converters/CloudEventsConverter.class \
{} \;
```


./debezium-core-2.4.1.Final.jar


at io.debezium.converters.CloudEventsConverter.<clinit>(CloudEventsConverter.java:131)

org.apache.kafka.connect.cli.ConnectStandalone.main

jar:file:/opt/kafka/2.13-3.6.0/libs/debezium-connector-sqlserver-2.4.1.Final.jar!/META-INF/services/io.debezium.converters.spi.CloudEventsProvider

ServiceLoader : L.1230
if (service.isAssignableFrom(clazz))

service : interface io.debezium.converters.spi.CloudEventsProvider
clazz : class io.debezium.connector.sqlserver.converters.SqlServerCloudEventsProvider



io.debezium.connector.sqlserver.converters.SqlServerCloudEventsProvider