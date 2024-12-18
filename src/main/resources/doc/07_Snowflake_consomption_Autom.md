# Amorçage de la table de consultation finale

```sql
insert into sqlserver_ingest.BRZ.CUSTOMERS
    select distinct coalesce(t.record_content:payload.before.id::integer, t.record_content:payload.after.id::integer) as id
        , t.record_content:payload.after.email::text                                                         as email
        , t.record_content:payload.after.first_name::text                                                    as first_name
        , t.record_content:payload:after.last_name::text                                                     as last_name
    from sqlserver_ingest.landing.DBO_CUSTOMERS t
    where t.record_content:payload.op::text in ('r');
select * from sqlserver_ingest.brz.customers;
```

# Re-amorcage de la table en cas de dé-synchronisation

```sql
--------------------------------------------
-- latest snapshot queries
--------------------------------------------
truncate sqlserver_ingest.brz.customers;
--
insert into sqlserver_ingest.brz.customers
select a.id, a.first_name, a.last_name, a.email
  from (
    select distinct
      --  COALESCE renvoie un premier argument non NULL à partir de la liste d'arguments passée
      -- for the 'd' case, the 'id' will only come through in the 'before' section
      coalesce(t.record_content:payload:before.id::integer, t.record_content:payload:after.id::integer) as id
      , t.record_content:payload:source:ts_ms::timestamp                                     as ts_ms
      , t.record_content:payload.after.email::text                                           as email
      , t.record_content:payload:after.first_name::text                                      as first_name
      , t.record_content:payload:after.last_name::text                                       as last_name
    from SQLSERVER_INGEST.LANDING.DBO_CUSTOMERS as t
  ) as a
  inner join (
    -- Find the latest timestamp for each id
    select coalesce(u.record_content:payload:before.id::integer, u.record_content:payload:after.id::integer) as id
        , MAX(u.record_content:payload:source:ts_ms::timestamp) AS max_timestamp
    from  SQLSERVER_INGEST.LANDING.DBO_CUSTOMERS as u
    group by id
  ) as latest
  on a.ts_ms = latest.max_timestamp and a.id = latest.id;
--
select * from sqlserver_ingest.brz.customers;
```

# Création du Stream SQLSERVER_INGEST.LANDING.DBO_CUSTOMERS_STRM

```sql
export SNOW_KAFKA_DB=sqlserver_ingest
export SNOW_KAFKA_SCHEMA=landing

cat <<EOF | tee $SNOW_SCRIPTS/02_create_stream.sql
use role sysadmin;
create or replace stream $SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA.DBO_CUSTOMERS_STRM
on table $SNOW_KAFKA_DB.$SNOW_KAFKA_SCHEMA.DBO_CUSTOMERS
append_only = true;
EOF
```

```sql
cat $SNOW_SCRIPTS/02_create_stream.sql | snowsql -o log_level=DEBUG -c example
```




# Création de la tache sqlserver_ingest.landing.CUSTOMERS_TASK
```sql
-- création d'une tache. La tâche est crée mais non démarrée.
CREATE TASK sqlserver_ingest.landing.CUSTOMERS_TASK  WAREHOUSE = wh_ingest
  WHEN system$stream_has_data('sqlserver_ingest.landing.DBO_CUSTOMERS_STRM')
  AS
    merge into sqlserver_ingest.BRZ.CUSTOMERS as c
    using (
        select
            t.record_content                                                                     as record_content
            , t.record_content:payload:op::text                                                    as op
            --  COALESCE renvoie un premier argument non NULL à partir de la liste d'arguments passée
            -- for the 'd' case, the 'id' will only come through in the 'before' section
            , coalesce(t.record_content:payload.before.id::integer, t.record_content:payload.after.id::integer) as id
            , t.record_content:payload.source.ts_ms::timestamp                                     as ts_ms
            , t.record_content:payload.after.email::text                                           as email
            , t.record_content:payload.after.first_name::text                                      as first_name
            , t.record_content:payload:after.last_name::text                                       as last_name
        from sqlserver_ingest.landing.DBO_CUSTOMERS_STRM t
        where t.record_content:payload.op::text in ('u', 'c', 'd')
        -- handle when a batch of records has multiple operations for one record (update, insert, delete)
        -- in this case, take the most recent based on primary key
        qualify row_number() over (
            partition by coalesce(t.record_content:payload.before.id::integer, t.record_content:payload.after.id::integer)
            -- combine ts_ms and pos since they may be duplicated in the debezium logs
            order by (t.record_content:payload.ts_ms::integer*100000000000000 + t.record_content:payload.source.ts_ms::integer) desc nulls last
        ) = 1
    ) as s on s.id = c.id
    when matched and op = 'd'
        then delete
    -- here we assume that if a record is deleted then recreated immediately, we should handle that as an update operation
    when matched and (op = 'u' or op = 'c')
        then
            update set c.id = s.id
            , c.first_name = s.first_name
            , c.last_name = s.last_name
            , c.email = s.email
    when not matched and op != 'd'
        then
            insert (id, first_name, last_name, email)
            values (s.id, s.first_name, s.last_name, s.email);
```


```ad-warning
Le role qui lance la tache doit en etre le propriétaire et avoir les droits "execute task" sur le compte
```
## Ajout des droits d'exécution de tache

```sql
use role accountadmin;
grant execute task on account to role sysadmin with grant option;
use role sysadmin;
show grants to role sysadmin;
show grants to role kafka_instance_1;
grant execute task on account to role kafka_instance_1;
show grants to role kafka_instance_1;
-- cannot launch task if not the owner
```
## Activation de la tache 
```sql
-- Lancement de la tache pour consommer le stream
use role sysadmin;
ALTER TASK sqlserver_ingest.landing.CUSTOMERS_TASK RESUME;
```

## troubleshoot 
```sql
show tasks like 'CUSTOMERS_TASK' in schema sqlserver_ingest.landing;
show grants to role SYSADMIN;
```

# else

```sql
cat <<EOF | snowsql -o log_level=DEBUG -c example
!set sql_delimiter=/
select coalesce(t.record_content:payload.before.id::integer, t.record_content:payload.after.id::integer) as id
	, t.record_content:payload.after.email::text                                                         as email
	, t.record_content:payload.after.first_name::text                                                    as first_name
	, t.record_content:payload:after.last_name::text                                                     as last_name
from sqlserver_ingest.landing.DBO_CUSTOMERS t
where t.record_content:payload.op::text in ('r');/
!set sql_delimiter=";"
EOF
```


```sql
cat <<EOF | tee $SNOW_SCRIPTS/03_mergeQuery.sql
merge into sqlserver_ingest.BRZ.CUSTOMERS as c
using (
select
t.record_content                                                                       as record_content
, t.record_content:payload:op::text                                                    as op
--  COALESCE renvoie un premier argument non NULL à partir de la liste d'arguments passée
-- for the 'd' case, the 'id' will only come through in the 'before' section
, coalesce(t.record_content:payload.before.id::integer, t.record_content:payload.after.id::integer) as id
, t.record_content:payload.source.ts_ms::timestamp                                     as ts_ms
, t.record_content:payload.after.email::text                                           as email
, t.record_content:payload.after.first_name::text                                      as first_name
, t.record_content:payload:after.last_name::text                                       as last_name
from sqlserver_ingest.landing.DBO_CUSTOMERS_STRM t
where t.record_content:payload.op::text in ('u', 'c', 'd')
-- handle when a batch of records has multiple operations for one record (update, insert, delete)
-- in this case, take the most recent based on primary key
qualify row_number() over (
partition by coalesce(t.record_content:payload.before.id::integer, t.record_content:payload.after.id::integer)
-- combine ts_ms and pos since they may be duplicated in the debezium logs
order by (t.record_content:payload.ts_ms::integer*100000000000000 + t.record_content:payload.source.pos::integer) desc nulls last
) = 1
) as s on s.id = c.id
when matched and op = 'd'
then delete
-- here we assume that if a record is deleted then recreated immediately, we should handle that as an update operation
when matched and (op = 'u' or op = 'c')
then
update set c.id = s.id
, c.first_name = s.first_name
, c.last_name = s.last_name
, c.email = s.email
when not matched and op != 'd'
then
insert (id, first_name, last_name, email)
values (s.id, s.first_name, s.last_name, s.email);
select * from sqlserver_ingest.BRZ.CUSTOMERS;
EOF
cat $SNOW_SCRIPTS/03_mergeQuery.sql | snowsql -o log_level=DEBUG -c example
```