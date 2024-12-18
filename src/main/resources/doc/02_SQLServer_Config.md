# Configure SQL Server

## Full Reset :

```sql
source .dbzrc
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
DECLARE @dbName NVARCHAR(100)
SET @dbName = '$SQLSERVER_DB_NAME'

USE master
DECLARE @SessionId INT
DECLARE @KillQuery NVARCHAR(100)

-- Récupérer les sessions à tuer
DECLARE sessions_cursor CURSOR FOR
SELECT session_id
FROM sys.dm_exec_sessions
WHERE database_id = DB_ID(@dbName)
    AND session_id <> @@SPID

-- Ouvrir le curseur
OPEN sessions_cursor

-- Initialiser la variable @SessionId
FETCH NEXT FROM sessions_cursor INTO @SessionId

-- Itérer sur les sessions et les tuer
WHILE @@FETCH_STATUS = 0
BEGIN
    -- Construire la requête KILL dynamiquement
    SET @KillQuery = 'KILL ' + CAST(@SessionId AS NVARCHAR(100))

    -- Exécuter la requête KILL dynamique
    EXEC sp_executesql @KillQuery

    -- Récupérer la session suivante
    FETCH NEXT FROM sessions_cursor INTO @SessionId
END

-- Fermer le curseur
CLOSE sessions_cursor

-- Libérer les ressources du curseur
DEALLOCATE sessions_cursor
GO

DROP DATABASE $SQLSERVER_DB_NAME;
GO
EOF
```


```sql
# to check that database has been removed
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
USE master
SELECT name
FROM sys.databases;
GO
EOF
```
OR
```sql
# to check that database has been removed
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
USE master
EXEC sp_databases;
GO
EOF
```

list les id des sessions sur la bdd:

```sql
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
DECLARE @dbName NVARCHAR(100)
SET @dbName = '$SQLSERVER_DB_NAME'

USE master

SELECT 'KILL ' + CONVERT(VARCHAR(100), session_id)
FROM sys.dm_exec_sessions
WHERE database_id = db_id(@dbName)
AND session_id <> @@spid
EOF
```
.... des lignes suivantes :

## Create tables, populate and activate CDC on MS SqlServer 
https://debezium.io/documentation/reference/stable/connectors/sqlserver.html#setting-up-sqlserver

```sh
# https://learn.microsoft.com/en-us/sql/tools/sqlcmd/sqlcmd-connect-database-engine?view=sql-server-ver16
sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
```


```sh
source .dbzrc
cat << EOF | tee inventory.sql
-- Create the test database
USE master;
GO
CREATE DATABASE $SQLSERVER_DB_NAME;
GO

USE $SQLSERVER_DB_NAME;
-- Create and populate our products using a single insert with many rows
CREATE TABLE products (
  id INTEGER IDENTITY(101,1) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(512),
  weight FLOAT
);
INSERT INTO products(name,description,weight)  VALUES ('scooter','Small 2-wheel scooter',3.14);
INSERT INTO products(name,description,weight)  VALUES ('car battery','12V car battery',8.1);
INSERT INTO products(name,description,weight)  VALUES ('12-pack drill bits','12-pack of drill bits with sizes ranging from #40 to #3',0.8);
INSERT INTO products(name,description,weight)  VALUES ('hammer','12oz carpenter''s hammer',0.75);
INSERT INTO products(name,description,weight)  VALUES ('hammer','14oz carpenter''s hammer',0.875);
INSERT INTO products(name,description,weight)  VALUES ('hammer','16oz carpenter''s hammer',1.0);
INSERT INTO products(name,description,weight)  VALUES ('rocks','box of assorted rocks',5.3);
INSERT INTO products(name,description,weight)  VALUES ('jacket','water resistent black wind breaker',0.1);
INSERT INTO products(name,description,weight)  VALUES ('spare tire','24 inch spare tire',22.2);

-- Create and populate the products on hand using multiple inserts
CREATE TABLE products_on_hand (
  product_id INTEGER NOT NULL PRIMARY KEY,
  quantity INTEGER NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products(id)
);
INSERT INTO products_on_hand VALUES (101,3);
INSERT INTO products_on_hand VALUES (102,8);
INSERT INTO products_on_hand VALUES (103,18);
INSERT INTO products_on_hand VALUES (104,4);
INSERT INTO products_on_hand VALUES (105,5);
INSERT INTO products_on_hand VALUES (106,0);
INSERT INTO products_on_hand VALUES (107,44);
INSERT INTO products_on_hand VALUES (108,2);
INSERT INTO products_on_hand VALUES (109,5);
GO

-- Create some customers ...
CREATE TABLE customers (
  id INTEGER IDENTITY(1001,1) NOT NULL PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE
);
INSERT INTO customers(first_name,last_name,email)  VALUES ('Sally','Thomas','sally.thomas@acme.com');
INSERT INTO customers(first_name,last_name,email)  VALUES ('George','Bailey','gbailey@foobar.com');
INSERT INTO customers(first_name,last_name,email)  VALUES ('Edward','Walker','ed@walker.com');
INSERT INTO customers(first_name,last_name,email)  VALUES ('Anne','Kretchmar','annek@noanswer.org');
GO

-- Create some very simple orders
CREATE TABLE orders (
  id INTEGER IDENTITY(10001,1) NOT NULL PRIMARY KEY,
  order_date DATE NOT NULL,
  purchaser INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  FOREIGN KEY (purchaser) REFERENCES customers(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);
INSERT INTO orders(order_date,purchaser,quantity,product_id)  VALUES ('16-JAN-2016', 1001, 1, 102);
INSERT INTO orders(order_date,purchaser,quantity,product_id)  VALUES ('17-JAN-2016', 1002, 2, 105);
INSERT INTO orders(order_date,purchaser,quantity,product_id)  VALUES ('19-FEB-2016', 1002, 2, 106);
INSERT INTO orders(order_date,purchaser,quantity,product_id)  VALUES ('21-FEB-2016', 1003, 1, 107);
GO

-- activate CDC
EXEC sys.sp_cdc_enable_db;
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'products', @role_name = NULL, @supports_net_changes = 0;
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'products_on_hand', @role_name = NULL, @supports_net_changes = 0;
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'customers', @role_name = NULL, @supports_net_changes = 0;
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'orders', @role_name = NULL, @supports_net_changes = 0;
GO

-- list databases 
USE master
EXEC sp_databases;
GO
EOF
```
[[inventory.sql]]

Lancement du script
```sh
cat inventory.sql | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
```

==>
```
Job 'cdc.testDB_capture' started successfully.
Job 'cdc.testDB_cleanup' started successfully.
```
![[Pasted image 20240513142759.png]]

KO :
```sql
# pour visualiser les changements sur la table "customers"
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
DECLARE @start_lsn binary(10)
SET @start_lsn = 0x0000002800000B950042
SELECT * FROM $SQLSERVER_DB_NAME.cdc.fn_cdc_get_all_changes_dbo_customers(@start_lsn,NULL,NULL);
GO
EOF
```
KO :
```sql
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
DECLARE @from_lsn binary(10), @to_lsn binary(10);  
SET @from_lsn = $SQLSERVER_DB_NAME.sys.fn_cdc_get_min_lsn('dbo_custumers');  
SET @to_lsn   = $SQLSERVER_DB_NAME.sys.fn_cdc_get_max_lsn();  
SELECT * FROM $SQLSERVER_DB_NAME.cdc.fn_cdc_get_all_changes_dbo_customers(@from_lsn, @to_lsn, N'all update old');  
GO
EOF
```

```sql
# pour visualiser les tables et les colonnes de capture
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
SELECT ct.capture_instance, cc.column_name, cc.column_type,*
FROM $SQLSERVER_DB_NAME.cdc.change_tables AS ct
LEFT JOIN $SQLSERVER_DB_NAME.cdc.captured_columns AS cc
    ON ct.object_id = cc.object_id
ORDER BY ct.capture_instance;
EOF
```
```ad-example
title: Resultat
collapse: close
|capture_instance|column_name|column_type|object_id|version|source_object_id|capture_instance|start_lsn|end_lsn|supports_net_changes|has_drop_pending|role_name|index_name|filegroup_name|create_date|partition_switch|object_id|column_name|column_id|column_type|column_ordinal|is_computed|masking_function|
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
|dbo_customers|id|int|1605580758|0|981578535|dbo_customers|0x0000002800000AD30042|NULL|0|NULL|NULL|PK__customer__3213E83F73A2E5AE|NULL|2024-05-13 16:17:11.730|1|1605580758|id|1|int|1|0|NULL|
|dbo_customers|first_name|varchar|1605580758|0|981578535|dbo_customers|0x0000002800000AD30042|NULL|0|NULL|NULL|PK__customer__3213E83F73A2E5AE|NULL|2024-05-13 16:17:11.730|1|1605580758|first_name|2|varchar|2|0|NULL|
|dbo_customers|last_name|varchar|1605580758|0|981578535|dbo_customers|0x0000002800000AD30042|NULL|0|NULL|NULL|PK__customer__3213E83F73A2E5AE|NULL|2024-05-13 16:17:11.730|1|1605580758|last_name|3|varchar|3|0|NULL|
|dbo_customers|email|varchar|1605580758|0|981578535|dbo_customers|0x0000002800000AD30042|NULL|0|NULL|NULL|PK__customer__3213E83F73A2E5AE|NULL|2024-05-13 16:17:11.730|1|1605580758|email|4|varchar|4|0|NULL|
|dbo_orders|id|int|1685581043|0|1029578706|dbo_orders|0x00000029000003440066|NULL|0|NULL|NULL|PK__orders__3213E83F76C28BF2|NULL|2024-05-13 16:17:12.267|1|1685581043|id|1|int|1|0|NULL|
|dbo_orders|order_date|date|1685581043|0|1029578706|dbo_orders|0x00000029000003440066|NULL|0|NULL|NULL|PK__orders__3213E83F76C28BF2|NULL|2024-05-13 16:17:12.267|1|1685581043|order_date|2|date|2|0|NULL|
|dbo_orders|purchaser|int|1685581043|0|1029578706|dbo_orders|0x00000029000003440066|NULL|0|NULL|NULL|PK__orders__3213E83F76C28BF2|NULL|2024-05-13 16:17:12.267|1|1685581043|purchaser|3|int|3|0|NULL|
|dbo_orders|quantity|int|1685581043|0|1029578706|dbo_orders|0x00000029000003440066|NULL|0|NULL|NULL|PK__orders__3213E83F76C28BF2|NULL|2024-05-13 16:17:12.267|1|1685581043|quantity|4|int|4|0|NULL|
|dbo_orders|product_id|int|1685581043|0|1029578706|dbo_orders|0x00000029000003440066|NULL|0|NULL|NULL|PK__orders__3213E83F76C28BF2|NULL|2024-05-13 16:17:12.267|1|1685581043|product_id|5|int|5|0|NULL|
|dbo_products|id|int|1445580188|0|901578250|dbo_products|0x0000002700000A1F005A|NULL|0|NULL|NULL|PK__products__3213E83FDF63FB8D|NULL|2024-05-13 16:17:04.700|1|1445580188|id|1|int|1|0|NULL|
|dbo_products|name|varchar|1445580188|0|901578250|dbo_products|0x0000002700000A1F005A|NULL|0|NULL|NULL|PK__products__3213E83FDF63FB8D|NULL|2024-05-13 16:17:04.700|1|1445580188|name|2|varchar|2|0|NULL|
|dbo_products|description|varchar|1445580188|0|901578250|dbo_products|0x0000002700000A1F005A|NULL|0|NULL|NULL|PK__products__3213E83FDF63FB8D|NULL|2024-05-13 16:17:04.700|1|1445580188|description|3|varchar|3|0|NULL|
|dbo_products|weight|float|1445580188|0|901578250|dbo_products|0x0000002700000A1F005A|NULL|0|NULL|NULL|PK__products__3213E83FDF63FB8D|NULL|2024-05-13 16:17:04.700|1|1445580188|weight|4|float|4|0|NULL|
|dbo_products_on_hand|product_id|int|1525580473|0|933578364|dbo_products_on_hand|0x00000028000002A60039|NULL|0|NULL|NULL|PK__products__47027DF5030EE310|NULL|2024-05-13 16:17:11.300|1|1525580473|product_id|1|int|1|0|NULL|
|dbo_products_on_hand|quantity|int|1525580473|0|933578364|dbo_products_on_hand|0x00000028000002A60039|NULL|0|NULL|NULL|PK__products__47027DF5030EE310|NULL|2024-05-13 16:17:11.300|1|1525580473|quantity|2|int|2|0|NULL|
```

```sql
# pour visualiser les tables et les colonnes de capture
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
EXEC $SQLSERVER_DB_NAME.sys.sp_cdc_help_change_data_capture;
EOF
```
```ad-example
title: Resultat
collapse: close
| source_schema | source_table     | capture_instance     | object_id  | source_object_id | start_lsn              | end_lsn | supports_net_changes | has_drop_pending | role_name | index_name                     | filegroup_name | create_date             | index_column_list | captured_column_list                                      |
| ------------- | ---------------- | -------------------- | ---------- | ---------------- | ---------------------- | ------- | -------------------- | ---------------- | --------- | ------------------------------ | -------------- | ----------------------- | ----------------- | --------------------------------------------------------- |
| dbo           | customers        | dbo_customers        | 1605580758 | 981578535        | 0x0000002800000AD30042 | NULL    | 0                    | NULL             | NULL      | PK__customer__3213E83F73A2E5AE | NULL           | 2024-05-13 16:17:11.730 | [id]              | [id], [first_name], [last_name], [email]                  |
| dbo           | orders           | dbo_orders           | 1685581043 | 1029578706       | 0x00000029000003440066 | NULL    | 0                    | NULL             | NULL      | PK__orders__3213E83F76C28BF2   | NULL           | 2024-05-13 16:17:12.267 | [id]              | [id], [order_date], [purchaser], [quantity], [product_id] |
| dbo           | products         | dbo_products         | 1445580188 | 901578250        | 0x0000002700000A1F005A | NULL    | 0                    | NULL             | NULL      | PK__products__3213E83FDF63FB8D | NULL           | 2024-05-13 16:17:04.700 | [id]              | [id], [name], [description], [weight]                     |
| dbo           | products_on_hand | dbo_products_on_hand | 1525580473 | 933578364        | 0x00000028000002A60039 | NULL    | 0                    | NULL             | NULL      | PK__products__47027DF5030EE310 | NULL           | 2024-05-13 16:17:11.300 | [product_id]      | [product_id], [quantity]                                  |

```


### make a change and show it

```sql
cat << EOF | sqlcmd -S $SQLSERVER_HOSTNAME -d master -U $SQLSERVER_USER -P $SQLSERVER_PWD
USE $SQLSERVER_DB_NAME;
GO
INSERT INTO dbo.customers(first_name,last_name,email)
  VALUES ('AnnA','POurshikin','annAP@noanswer.org');

UPDATE customers SET last_name='Poushkin'
	WHERE customers.first_name='AnnA';

select * from cdc.dbo_customers_CT;
EOF
```
```ad-example
title: Resultat
collapse: close
| __$start_lsn           | __$end_lsn | __$seqval              | __$operation | __$update_mask | id   | first_name | last_name  | email              | __$command_id |
| ---------------------- | ---------- | ---------------------- | ------------ | -------------- | ---- | ---------- | ---------- | ------------------ | ------------- |
| 0x0000002A000003B00005 | NULL       | 0x0000002A000003B00003 | 2            | 0x0F           | 1005 | AnnA       | POurshikin | annAP@noanswer.org | 1             |
| 0x0000002A00000D350003 | NULL       | 0x0000002A00000D350002 | 3            | 0x04           | 1005 | AnnA       | POurshikin | annAP@noanswer.org | 1             |
| 0x0000002A00000D350003 | NULL       | 0x0000002A00000D350002 | 4            | 0x04           | 1005 | AnnA       | Poushkin   | annAP@noanswer.org | 1             |
```
with 
`__$operation` : 
- 2 => Insert 
- 3/4 => Update (3=> val before / 4 val after)





### détail du jeu de test
```sql
-- Create the test database
CREATE DATABASE testDB;
GO
USE testDB;
```


```sql
-- Create and populate our products using a single insert with many rows
CREATE TABLE products (
  id INTEGER IDENTITY(101,1) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(512),
  weight FLOAT
);
INSERT INTO products(name,description,weight)
  VALUES ('scooter','Small 2-wheel scooter',3.14);
INSERT INTO products(name,description,weight)
  VALUES ('car battery','12V car battery',8.1);
INSERT INTO products(name,description,weight)
  VALUES ('12-pack drill bits','12-pack of drill bits with sizes ranging from #40 to #3',0.8);
INSERT INTO products(name,description,weight)
  VALUES ('hammer','12oz carpenter''s hammer',0.75);
INSERT INTO products(name,description,weight)
  VALUES ('hammer','14oz carpenter''s hammer',0.875);
INSERT INTO products(name,description,weight)
  VALUES ('hammer','16oz carpenter''s hammer',1.0);
INSERT INTO products(name,description,weight)
  VALUES ('rocks','box of assorted rocks',5.3);
INSERT INTO products(name,description,weight)
  VALUES ('jacket','water resistent black wind breaker',0.1);
INSERT INTO products(name,description,weight)
  VALUES ('spare tire','24 inch spare tire',22.2);
```

```sql
-- Create and populate the products on hand using multiple inserts
CREATE TABLE products_on_hand (
  product_id INTEGER NOT NULL PRIMARY KEY,
  quantity INTEGER NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products(id)
);
INSERT INTO products_on_hand VALUES (101,3);
INSERT INTO products_on_hand VALUES (102,8);
INSERT INTO products_on_hand VALUES (103,18);
INSERT INTO products_on_hand VALUES (104,4);
INSERT INTO products_on_hand VALUES (105,5);
INSERT INTO products_on_hand VALUES (106,0);
INSERT INTO products_on_hand VALUES (107,44);
INSERT INTO products_on_hand VALUES (108,2);
INSERT INTO products_on_hand VALUES (109,5);
```

```sql
-- Create some customers ...
CREATE TABLE customers (
  id INTEGER IDENTITY(1001,1) NOT NULL PRIMARY KEY,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE
);
INSERT INTO customers(first_name,last_name,email)
  VALUES ('Sally','Thomas','sally.thomas@acme.com');
INSERT INTO customers(first_name,last_name,email)
  VALUES ('George','Bailey','gbailey@foobar.com');
INSERT INTO customers(first_name,last_name,email)
  VALUES ('Edward','Walker','ed@walker.com');
INSERT INTO customers(first_name,last_name,email)
  VALUES ('Anne','Kretchmar','annek@noanswer.org');
```

```sql
-- Create some very simple orders
CREATE TABLE orders (
  id INTEGER IDENTITY(10001,1) NOT NULL PRIMARY KEY,
  order_date DATE NOT NULL,
  purchaser INTEGER NOT NULL,
  quantity INTEGER NOT NULL,
  product_id INTEGER NOT NULL,
  FOREIGN KEY (purchaser) REFERENCES customers(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('16-JAN-2016', 1001, 1, 102);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('17-JAN-2016', 1002, 2, 105);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('19-FEB-2016', 1002, 2, 106);
INSERT INTO orders(order_date,purchaser,quantity,product_id)
  VALUES ('21-FEB-2016', 1003, 1, 107);
GO
```

```sql
-- activate cdc on database
USE testDB;
EXEC sys.sp_cdc_enable_db;

-- activate cdc on table products
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'products', @role_name = NULL, @supports_net_changes = 0;
-- activate cdc on table products_on_hand
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'products_on_hand', @role_name = NULL, @supports_net_changes = 0;
-- activate cdc on table customers
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'customers', @role_name = NULL, @supports_net_changes = 0;
-- activate cdc on table orders
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'orders', @role_name = NULL, @supports_net_changes = 0;
```

```sql
EXEC sys.sp_cdc_help_change_data_capture;
GO
```