# Configure Snowflake
## Install snowSQL
https://docs.snowflake.com/en/user-guide/snowsql-install-config#installing-snowsql-on-linux-using-the-installer
### Set Variables

```sh
export SNOWSQL_MIN_VER="31"
export SNOWSQL_MAJ_VER="1.2"
export SNOWSQL_VERSION=$SNOWSQL_MAJ_VER.$SNOWSQL_MIN_VER
export SNOWSQL_DEST=~/bin 
export SNOWSQL_LOGIN_SHELL=~/.bashrc
export SNOW_ACCOUNT="UA48835.eu-west-3.aws"
# this following line also works
# export SNOW_ACCOUNT="PDTWPID-IR78265"
# but this one does not
# export SNOW_ACCOUNT="PDTWPID.IR78265"
export SNOW_ADMIN_USER="afanon"
export SNOW_ADMIN_PWD="*****"
export SNOW_LANDING_USER="sqlserver_rep"
export SNOW_LANDING_PWD="*****"
```

### Download the installer
```sh
curl -O https://sfc-repo.snowflakecomputing.com/snowsql/bootstrap/$SNOWSQL_MAJ_VER/linux_x86_64/snowsql-$SNOWSQL_VERSION-linux_x86_64.bash

curl -O https://sfc-repo.snowflakecomputing.com/snowsql/bootstrap/$SNOWSQL_MAJ_VER/linux_x86_64/snowsql-$SNOWSQL_VERSION-linux_x86_64.bash.sig

gpg --keyserver hkp://keyserver.ubuntu.com --recv-keys 630D9F3CAB551AF3

# Verify the package signature.
gpg --verify snowsql-$SNOWSQL_VERSION-linux_x86_64.bash.sig snowsql-$SNOWSQL_VERSION-linux_x86_64.bash

gpg --delete-key "Snowflake Computing"
```

### Run the installer

```sh
bash snowsql-$SNOWSQL_VERSION-linux_x86_64.bash
source $SNOWSQL_LOGIN_SHELL
snowsql -v
```


```sh
snowsql -a $SNOW_ACCOUNT -u $SNOW_ADMIN_USER -o log_level=DEBUG
```

### Add a "~.snowsql/config"

```sh
sed -i "s|accountname =.*|accountname = $SNOW_ACCOUNT|" ~/.snowsql/config
sed -i "s|username =.*|username = $SNOW_ADMIN_USER|" ~/.snowsql/config
sed -i "s|password =.*|password = $SNOW_ADMIN_PWD|" ~/.snowsql/config
chmod 700 ~/.snowsql/config
cat ~/.snowsql/config
```

## Run snowSQL
```sh
snowsql -a <ACCOUNT_NAME> -u <USERNAME> -d <DATABASE_NAME> -w <WAREHOUSE_NAME> -r <REGION> -s <SCHEMA_NAME> -f <OUTPUT_FORMAT> -o <OUTPUT_FILE> -q "SHOW DATABASES;"
```

```sh
# parm -c 'example' refers to the connection defined in ~/.snowsql/config (see previous chapt.)
cat << EOF | snowsql -o log_level=DEBUG -c example
SHOW DATABASES;
EOF
```

```sh
# connect with 'example' connection
snowsql -o log_level=DEBUG -c example
```

```sql
!exit
```


## Create login Security Material
### Create RSA KeyPair
https://community.snowflake.com/s/article/How-to-verify-the-Public-Private-Key-Connectivity-using-SnowSQL

- Store P8 password
```sh
cat << EOF | tee .snwrc
export SNOW_P8_FILE_PWD='*****'
EOF
source .snwrc
```

- Generate private / public keys
	- From aes256 :
```sh
mkdir -p $SNOW_SCRIPTS
cd $SNOW_SCRIPTS

openssl genrsa 2048 | openssl pkcs8 -topk8 -v2 aes256 -inform PEM -out snow_rsa_key.p8 -passout env:SNOW_P8_FILE_PWD
openssl rsa -in snow_rsa_key.p8 -pubout -out snow_rsa_key.pub  -passin env:SNOW_P8_FILE_PWD
```

	- From default  :
```sh
mkdir -p $SNOW_SCRIPTS
cd $SNOW_SCRIPTS

openssl genrsa 2048 | openssl pkcs8 -topk8 -inform PEM -out snow_rsa_key.p8 -passout env:SNOW_P8_FILE_PWD
openssl rsa -in snow_rsa_key.p8 -pubout -out snow_rsa_key.pub  -passin env:SNOW_P8_FILE_PWD
```

- Format private / public keys
```sh
cd $SNOW_SCRIPTS
export SNOW_PRIV_KEY=$(grep -v PRIVATE snow_rsa_key.p8 | sed ':a;N;$!ba;s/\n//g')
export SNOW_PUB_KEY=$(grep -v PUBLIC snow_rsa_key.pub | sed ':a;N;$!ba;s/\n//g')

echo $SNOW_PRIV_KEY
echo $SNOW_PUB_KEY
```

### Add Public key to Snow User Key Store

```sh
echo "create user if not exists $SNOW_LANDING_USER" | snowsql -o log_level=DEBUG -c example
echo "alter user $SNOW_LANDING_USER set rsa_public_key='$SNOW_PUB_KEY'" \
| snowsql -o log_level=DEBUG -c example
```

```sh
cat <<EOF | snowsql -o log_level=DEBUG -c example
create user if not exists $SNOW_LANDING_USER;
alter user $SNOW_LANDING_USER set rsa_public_key='$SNOW_PUB_KEY';
EOF
```

### Test user landing user

```sh
cat <<EOF | snowsql -a $SNOW_ACCOUNT -u $SNOW_LANDING_USER -o log_level=DEBUG --private-key-path $SNOW_SCRIPTS/snow_rsa_key.p8
DESC USER $SNOW_LANDING_USER;
SELECT TRIM((SELECT "value" FROM TABLE(RESULT_SCAN(LAST_QUERY_ID()))
  WHERE "property" = 'RSA_PUBLIC_KEY_FP'), 'SHA256:');
EOF
```

```sh
echo "drop user $SNOW_LANDING_USER" | snowsql -o log_level=DEBUG -c example
```

## Create BDD, Warehouse, roles and user 

### Create Kafka Role for Snowflake 

```sql
-- Use a role that can create and manage roles and privileges.
USE ROLE securityadmin;

-- Create a Snowflake role with the privileges to work with the connector.
CREATE ROLE kafka_connector_$snow_role_1;

-- Grant privileges on the database.
GRANT USAGE ON DATABASE $snow_kafka_db TO ROLE kafka_connector_$snow_role_1;

-- Grant privileges on the schema.
GRANT USAGE ON SCHEMA $snow_kafka_schema TO ROLE kafka_connector_$snow_role_1;
GRANT CREATE TABLE ON SCHEMA $snow_kafka_schema TO ROLE kafka_connector_$snow_role_1;
GRANT CREATE STAGE ON SCHEMA $snow_kafka_schema TO ROLE kafka_connector_$snow_role_1;
GRANT CREATE PIPE ON SCHEMA $snow_kafka_schema TO ROLE kafka_connector_$snow_role_1;

-- Only required if the Kafka connector will load data into an existing table.
GRANT OWNERSHIP ON TABLE existing_table1 TO ROLE kafka_connector_$snow_role_1;

-- Only required if the Kafka connector will stage data files in an existing internal stage: (not recommended).
GRANT READ, WRITE ON STAGE existing_stage1 TO ROLE kafka_connector_$snow_role_1;

-- Grant the custom role to an existing user.
GRANT ROLE kafka_connector_$snow_role_1 TO USER kafka_connector_user_1;

-- Set the custom role as the default role for the user.
-- If you encounter an 'Insufficient privileges' error, verify the role that has the OWNERSHIP privilege on the user.
ALTER USER kafka_connector_user_1 SET DEFAULT_ROLE = kafka_connector_$snow_role_1;
```

```ad-warning
title: Drop database

```sql
export SNOW_KAFKA_DB=sqlserver_ingest
export SNOW_KAFKA_LANDING_ROLE=kafka_instance_1
export SNOW_LANDING_USER=sqlserver_rep
cat << EOF | snowsql -o log_level=DEBUG -c example
use role sysadmin;
DROP DATABASE $SNOW_KAFKA_DB;
use role securityadmin;
drop user $SNOW_LANDING_USER;
drop role $SNOW_KAFKA_LANDING_ROLE;
EOF
```



```sql
cd $SNOW_SCRIPTS
export SNOW_PRIV_KEY=$(grep -v PRIVATE snow_rsa_key.p8 | sed ':a;N;$!ba;s/\n//g')
export SNOW_PUB_KEY=$(grep -v PUBLIC snow_rsa_key.pub | sed ':a;N;$!ba;s/\n//g')
# export SNOW_KAFKA_DB=sqlserver_ingest
export SNOW_KAFKA_DB=vwc2bdd052_e2i5_dmh_dwh
export SNOW_KAFKA_SCHEMA=landing
export SNOW_TARGET_SCHEMA=dbo
export SNOW_KAFKA_LANDING_ROLE=kafka_instance_1
export SNOW_WAREHOUSE=wh_ingest
export SNOW_LANDING_USER=sqlserver_rep

cat << EOF | tee $SNOW_SCRIPTS/01_dbz_snow_landing_structure.sql
use role sysadmin;
create warehouse if not exists $SNOW_WAREHOUSE warehouse_size = xsmall;
create database if not exists $SNOW_KAFKA_DB;
create schema if not exists $SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA;
create schema if not exists $SNOW_KAFKA_DB.$SNOW_TARGET_SCHEMA;

-- Use a role that can create and manage roles and privileges.
use role securityadmin;
-- Create a Snowflake role with the privileges to work with the connector.
create role if not exists $SNOW_KAFKA_LANDING_ROLE;
-- Grant privileges on the database.
grant usage on database $SNOW_KAFKA_DB to role $SNOW_KAFKA_LANDING_ROLE;
-- Grant privileges on the schema.
grant usage on schema $SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA to role $SNOW_KAFKA_LANDING_ROLE;
grant create table on schema $SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA to role $SNOW_KAFKA_LANDING_ROLE;
grant create stage on schema $SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA to role $SNOW_KAFKA_LANDING_ROLE;
grant create pipe on schema $SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA to role $SNOW_KAFKA_LANDING_ROLE;
--grant all on database sqlserver_ingest to role $SNOW_KAFKA_LANDING_ROLE;
--grant all on schema sqlserver_ingest.landing to role $SNOW_KAFKA_LANDING_ROLE;
-- Grant privileges on the VWH.
grant all on warehouse $SNOW_WAREHOUSE to role $SNOW_KAFKA_LANDING_ROLE;

create user if not exists $SNOW_LANDING_USER identified by 'XXXXXXXXXXXXXXXXX';
alter user $SNOW_LANDING_USER set PASSWORD = '$SNOW_LANDING_PWD' MUST_CHANGE_PASSWORD = false;
alter user $SNOW_LANDING_USER set rsa_public_key='$SNOW_PUB_KEY';

grant role $SNOW_KAFKA_LANDING_ROLE to user $SNOW_LANDING_USER;
grant role $SNOW_KAFKA_LANDING_ROLE to user $SNOW_ADMIN_USER;
grant role $SNOW_KAFKA_LANDING_ROLE to role accountadmin;
grant role $SNOW_KAFKA_LANDING_ROLE to role sysadmin;
alter user $SNOW_LANDING_USER set DEFAULT_WAREHOUSE=$SNOW_WAREHOUSE;
alter user $SNOW_LANDING_USER set DEFAULT_NAMESPACE=$SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA;
alter user $SNOW_LANDING_USER set default_role=$SNOW_KAFKA_LANDING_ROLE;

desc user $SNOW_LANDING_USER;
select trim((select "value" from table(RESULT_SCAN(LAST_QUERY_ID()))
  where "property" = 'RSA_PUBLIC_KEY_FP'), 'SHA256:');

EOF
```

```sh
cat $SNOW_SCRIPTS/01_dbz_snow_landing_structure.sql | snowsql -o log_level=DEBUG -c example
```


```sql
cat <<EOF | snowsql -o log_level=DEBUG -c example
use role sysadmin;
create or replace table sqlserver_ingest.BRZ.CUSTOMERS (
id int NOT NULL,
first_name varchar(255) NOT NULL,
last_name varchar(255) NOT NULL,
email varchar(255) NOT NULL,
CONSTRAINT PK__customer UNIQUE (id),
CONSTRAINT UQ__customer UNIQUE (email)
);
-- fowing is now done in consumption file Snowflake_consomption
-- -- Creation du Stream SQLSERVER_INGEST.LANDING.DBO_CUSTOMERS_STRM
-- create or replace stream sqlserver_ingest.landing.DBO_CUSTOMERS_STRM
-- on table sqlserver_ingest.landing.DBO_CUSTOMERS
-- append_only = true;
EOF
```


# Troubleshooting
## SnowConnect

```sh
export SNOW_CONNECT_VERS=2.1.2

wget https://repo1.maven.org/maven2/com/snowflake/snowflake-kafka-connector/$SNOW_CONNECT_VERS/snowflake-kafka-connector-$SNOW_CONNECT_VERS.jar
```