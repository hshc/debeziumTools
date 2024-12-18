# Init Global Vars 

```sh
cat << EOF | tee .dbzrc
export SQLSERVER_HOSTNAME=localhost
export SQLSERVER_PORT=1433
export SQLSERVER_USER=sa
export SQLSERVER_PWD='*****'
export SQLSERVER_DB_NAME=debezium

export SNOWSQL_MIN_VER="31"
export SNOWSQL_MAJ_VER="1.2"
export SNOWSQL_VERSION=$SNOWSQL_MAJ_VER.$SNOWSQL_MIN_VER
export SNOWSQL_DEST=~/bin 
export SNOWSQL_LOGIN_SHELL=~/.bashrc

export SNOW_ACCOUNT="UA48835.eu-west-3.aws"
# this following line also works
# export SNOW_ACCOUNT="PDTWPID-IR78265"
# but this one does not

# export SNOW_ACCOUNT="PDTWPID-IR78265"
# export SNOW_ADMIN_USER="afanon"
# export SNOW_ADMIN_PWD="******"
export SNOW_ACCOUNT="MD-LUFFYBOAT"
export SNOW_ADMIN_USER="tcoton"
export SNOW_ADMIN_PWD="*****"
export SNOW_LANDING_USER="sqlserver_rep"
export SNOW_LANDING_PWD="*****"

export KAFKA_MAJ_VER="3.6.0"
export KAFKA_MIN_VER="2.13"
export KAFKA_VERSION=$KAFKA_MIN_VER-$KAFKA_MAJ_VER
export KAFKA_HOME=/opt/kafka/$KAFKA_VERSION

export KAFKA_SERVER_HOST=localhost
export KAFKA_SERVER_PORT=9092
export KAFKA_SERVER_LOGS=/tmp/kafka-logs

export KAFKA_CONNECT_API_HOST=localhost
export KAFKA_CONNECT_API_PORT=8083
export KAFKA_OUTPUT_TRACES=/tmp/kafka_output_logs
# mkdir -p $KAFKA_OUTPUT_TRACES

export SNOW_SINK_CONNECTOR_VERSION="2.2.2"
export DBZ_SQLSERVER_SRC_CONNECTOR_VERSION="2.4.1.Final"

export SNOW_P8_FILE_PWD='*****'
export SNOWSQL_PRIVATE_KEY_PASSPHRASE=$SNOW_P8_FILE_PWD
export SNOW_SCRIPTS=$HOME/dbz_snow_scripts

export S3_BUCKET=""
export KAFKA_SERVER_HOST_AWS=b-2.debeziumcluster.oy39o9.c2.kafka.eu-west-3.amazonaws.com
EOF
chmod 600 .dbzrc
source .dbzrc
mkdir -p $KAFKA_OUTPUT_TRACES
```
