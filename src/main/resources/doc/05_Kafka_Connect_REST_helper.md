# Kafka Connect REST API
https://kafka.apache.org/documentation/#connect_rest
https://developer.confluent.io/courses/kafka-connect/rest-api/
https://docs.confluent.io/platform/current/connect/references/restapi.html#connectors
https://www.youtube.com/watch?v=4xWPDXhBi3g

## List connector plugins
```sh
# list connector plugins
curl $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connector-plugins \
| jq '.'
```

## Connectors 
### Add connector config for SQL Server
```sh
# Add connector config for SQL Server 
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/ -d @register-dbz-sqlserver.json
```

### List connectors
```shell
# list connectors
curl -H "Accept:application/json" $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/ \
| jq '.'
```

```sh
# list connectors
curl -s -X GET "$KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors"
```
### Show one config 
```sh
# show one connector config
curl -s -X GET -H "Accept:application/json" $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/dbz-sqlserver-connector \
| jq '.'
```

```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X GET -H "Accept:application/json" $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\} \
| jq '.'

```

### List infos and status
```sh
curl -s "$KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/?expand=info" \
| jq '.'
```

```sh
curl -s "$KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/?expand=info" \
| jq '. | to_entries[] | [ .value.info.type, .key, .value.info.tasks[].task, .value.info.config."connector.class"] |join(":|:")' | column -s : -t| sed 's|\"||g' | sort
```


```sh
curl -s "$KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/?expand=info&expand=status" \
| jq '.'
```

```sh
curl -s "$KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/?expand=status" \
| jq '. | to_entries[] | [.key, .value.status.type, .value.status.connector.state] |join(":|:")' | column -s : -t| sed 's|\"||g' | sort

```

### Pause a Connector
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X PUT $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/pause
```
### Stop a Connector
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X PUT $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/stop
```
### Resume a Connector
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X PUT $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/resume
```
### Restart a Connector
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X POST $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/restart?includeTasks=true$onlyFailed=true
```

## Config

### Show one config 
```sh
# show one connector config
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X GET $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\} \
| jq '.'
```


## Topics
### List the topics of a Connector

```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X GET $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/topics \
| jq '.'
```
```ad-example
```json
{
  "dbz-sqlserver-connector": {
    "topics": [
      "dbzsqlserver",
      "dbzsqlserver.debezium.dbo.customers"
    ]
  }
}
```
```ad-attention
If a topic offset is at his max, the topic is not return in the list. In other words, only active topics (that is, topics containing fresh data) are in the returned list
```


### Read a choosen topic 


--offset <String: consume offset> : 
The offset to consume from (a non-negative number), or 'earliest' which means from beginning, or 'latest' which means from end (default: latest)

```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X GET $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/topics \
| jq '. | to_entries[] | .value.topics[]' \
| peco \
| xargs -I {topic} $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT --topic {topic} --partition 0 --offset 4 | tail -n +2 
```
```ad-example
title: Resultat
collapse: close
```json
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbzsqlserver.debezium.dbo.customers.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbzsqlserver.debezium.dbo.customers.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"version"},{"type":"string","optional":false,"field":"connector"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"ts_ms"},{"type":"string","optional":true,"name":"io.debezium.data.Enum","version":1,"parameters":{"allowed":"true,last,false,incremental"},"default":"false","field":"snapshot"},{"type":"string","optional":false,"field":"db"},{"type":"string","optional":true,"field":"sequence"},{"type":"string","optional":false,"field":"schema"},{"type":"string","optional":false,"field":"table"},{"type":"string","optional":true,"field":"change_lsn"},{"type":"string","optional":true,"field":"commit_lsn"},{"type":"int64","optional":true,"field":"event_serial_no"}],"optional":false,"name":"io.debezium.connector.sqlserver.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"id"},{"type":"int64","optional":false,"field":"total_order"},{"type":"int64","optional":false,"field":"data_collection_order"}],"optional":true,"name":"event.block","version":1,"field":"transaction"}],"optional":false,"name":"dbzsqlserver.debezium.dbo.customers.Envelope","version":1},"payload":{"before":null,"after":{"id":1005,"first_name":"AnnA","last_name":"POurshikin","email":"annAP@noanswer.org"},"source":{"version":"2.4.1.Final","connector":"sqlserver","name":"dbzsqlserver","ts_ms":1716469277173,"snapshot":"false","db":"debezium","sequence":null,"schema":"dbo","table":"customers","change_lsn":"0000002b:00000557:0003","commit_lsn":"0000002b:00000557:0005","event_serial_no":1},"op":"c","ts_ms":1716469280366,"transaction":null}}
{"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbzsqlserver.debezium.dbo.customers.Value","field":"before"},{"type":"struct","fields":[{"type":"int32","optional":false,"field":"id"},{"type":"string","optional":false,"field":"first_name"},{"type":"string","optional":false,"field":"last_name"},{"type":"string","optional":false,"field":"email"}],"optional":true,"name":"dbzsqlserver.debezium.dbo.customers.Value","field":"after"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"version"},{"type":"string","optional":false,"field":"connector"},{"type":"string","optional":false,"field":"name"},{"type":"int64","optional":false,"field":"ts_ms"},{"type":"string","optional":true,"name":"io.debezium.data.Enum","version":1,"parameters":{"allowed":"true,last,false,incremental"},"default":"false","field":"snapshot"},{"type":"string","optional":false,"field":"db"},{"type":"string","optional":true,"field":"sequence"},{"type":"string","optional":false,"field":"schema"},{"type":"string","optional":false,"field":"table"},{"type":"string","optional":true,"field":"change_lsn"},{"type":"string","optional":true,"field":"commit_lsn"},{"type":"int64","optional":true,"field":"event_serial_no"}],"optional":false,"name":"io.debezium.connector.sqlserver.Source","field":"source"},{"type":"string","optional":false,"field":"op"},{"type":"int64","optional":true,"field":"ts_ms"},{"type":"struct","fields":[{"type":"string","optional":false,"field":"id"},{"type":"int64","optional":false,"field":"total_order"},{"type":"int64","optional":false,"field":"data_collection_order"}],"optional":true,"name":"event.block","version":1,"field":"transaction"}],"optional":false,"name":"dbzsqlserver.debezium.dbo.customers.Envelope","version":1},"payload":{"before":{"id":1005,"first_name":"AnnA","last_name":"POurshikin","email":"annAP@noanswer.org"},"after":{"id":1005,"first_name":"AnnA","last_name":"Poushkin","email":"annAP@noanswer.org"},"source":{"version":"2.4.1.Final","connector":"sqlserver","name":"dbzsqlserver","ts_ms":1716469277177,"snapshot":"false","db":"debezium","sequence":null,"schema":"dbo","table":"customers","change_lsn":"0000002b:00000559:0002","commit_lsn":"0000002b:00000559:0003","event_serial_no":2},"op":"u","ts_ms":1716469280373,"transaction":null}}
```
#### From begining
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X GET $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/topics \
| jq '. | to_entries[] | .value.topics[]' \
| peco \
| xargs -I {topic} $KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT --topic {topic} --from-beginning | tail -n +2 | jq '.'
```




```sh
cd $KAFKA_HOME
# show the DDL changes
bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT --topic dbzsqlserver --from-beginning 
```


### Describe a choosen topic
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors \
| jq '.[]' \
| peco \
| xargs -I {name} curl -s -X GET $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/topics \
| jq '. | to_entries[] | .value.topics[]' \
| peco \
| xargs -I {topic} $KAFKA_HOME/bin/kafka-topics.sh --describe --topic {topic} --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT
```



### Empty the set of active topics of a connector
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors | \
jq '.[]' | \
peco | \
xargs -I {name} curl -s -X PUT $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/topics/reset
```


## Offsets
### List the offsets 
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors | \
jq '.[]' | \
peco | \
xargs -I {name} curl -s -X GET $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/offsets \
| jq '.'
```

Exemple de valeurs initiales
```ad-example
```json
{
  "offsets": [
    {
      "partition": {
        "server": "dbzsqlserver",
        "database": "debezium"
      },
      "offset": {
        "commit_lsn": "0000002b:00000ba3:0003",
        "snapshot": true,
        "snapshot_completed": true
      }
    }
  ]
}
```


Exemple après un create / update :
```ad-example 
```json
{
  "offsets": [
    {
      "partition": {
        "server": "dbzsqlserver",
        "database": "debezium"
      },
      "offset": {
        "transaction_id": null,
        "event_serial_no": 2,
        "commit_lsn": "0000002b:00000559:0003",
        "change_lsn": "0000002b:00000559:0002"
      }
    }
  ]
}

```
### Delete the offsets 
The connector must exist and must be in the stopped state =>[`PUT /connectors/{name}/stop`](https://kafka.apache.org/documentation/#connect_stopconnector)



export KAFKA_CONNECT_API_HOST="b-2.debeziumcluster.dyagho.c2.kafka.eu-west-3.amazonaws.com"
export KAFKA_CONNECT_API_PORT="9092"

```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors | \
jq '.[]' | \
peco | \
xargs -I {name} curl -s -X PUT $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/stop
```

```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors | \
jq '.[]' | \
peco | \
xargs -I {name} curl -s -X DELETE $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/offsets | jq '.'
```

```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors | \
jq '.[]' | \
peco | \
xargs -I {name} curl -s -X PUT $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/resume
```

## Tasks
### List the tasks of a connector
```sh
curl -s $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors | \
jq '.[]' | \
peco | \
xargs -I {name} curl -s -X GET $KAFKA_CONNECT_API_HOST:$KAFKA_CONNECT_API_PORT/connectors/\{name\}/tasks \
| jq '.'
```


```sh
cd $KAFKA_HOME
bin/kafka-topics.sh --list --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT
```

```sh
cd $KAFKA_HOME
# show the DDL changes
bin/kafka-console-consumer.sh --topic dbzsqlserver --from-beginning --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT


bin/kafka-console-consumer.sh --topic $TOPIC --from-beginning --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT
```



```sh
cd $KAFKA_HOME
# show the CDC changes
bin/kafka-console-consumer.sh --topic dbzsqlserver.$SQLSERVER_DB_NAME.dbo.customers --from-beginning --bootstrap-server $KAFKA_SERVER_HOST:$KAFKA_SERVER_PORT
```






```sh
cd $KAFKA_HOME
bin/kafka-topics.sh --list --bootstrap-server $KAFKA_SERVER_HOST_AWS:$KAFKA_SERVER_PORT
```


```sh
cd $KAFKA_HOME

bin/kafka-console-consumer.sh --topic $TOPIC --from-beginning --bootstrap-server $KAFKA_SERVER_HOST_AWS:$KAFKA_SERVER_PORT
```






