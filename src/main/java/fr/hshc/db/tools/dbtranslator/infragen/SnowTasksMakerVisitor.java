package fr.hshc.db.tools.dbtranslator.infragen;

import java.io.IOException;
import java.util.Map;

import fr.hshc.db.antlr4.DDLParser;

public class SnowTasksMakerVisitor extends SnowCodeGeneratorGenericVisitor {
	/*
	 * create task sqlserver_ingest.landing.CUSTOMERS_TASK WAREHOUSE = wh_ingest
	 * when
	 * system$stream_has_data('sqlserver_ingest.landing.DBO_CUSTOMERS_STRM') as
	 * merge into sqlserver_ingest.BRZ.CUSTOMERS as c using ( select
	 * t.record_content as record_content , t.record_content:payload:op::text as
	 * op -- COALESCE renvoie un premier argument non NULL à partir de la liste
	 * d'arguments passée -- for the 'd' case, the 'id' will only come through
	 * in the 'before' section ,
	 * coalesce(t.record_content:payload.before.id::integer,
	 * t.record_content:payload.after.id::integer) as id ,
	 * t.record_content:payload.source.ts_ms::integer as ts_ms ,
	 * t.record_content:payload.after.email::text as email ,
	 * t.record_content:payload.after.first_name::text as first_name ,
	 * t.record_content:payload:after.last_name::text as last_name from
	 * sqlserver_ingest.landing.DBO_CUSTOMERS_STRM t where
	 * t.record_content:payload.op::text in ('u', 'c', 'd') -- handle when a
	 * batch of records has multiple operations for one record (update, insert,
	 * delete) -- in this case, take the most recent based on primary key
	 * qualify row_number() over ( partition by
	 * coalesce(t.record_content:payload.before.id::integer,
	 * t.record_content:payload.after.id::integer) -- combine ts_ms and pos
	 * since they may be duplicated in the debezium logs order by
	 * (t.record_content:payload.ts_ms::integer*100000000000000 +
	 * t.record_content:payload.source.ts_ms::integer) desc nulls last ) = 1 )
	 * as s on s.id = c.id when matched and op = 'd' then delete -- here we
	 * assume that if a record is deleted then recreated immediately, we should
	 * handle that as an update operation when matched and (op = 'u' or op =
	 * 'c') then update set c.id = s.id , c.first_name = s.first_name ,
	 * c.last_name = s.last_name , c.email = s.email when not matched and op !=
	 * 'd' then insert (id, first_name, last_name, email) values (s.id,
	 * s.first_name, s.last_name, s.email);
	 */
	private String						warehouse		= null;

	public SnowTasksMakerVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String landingSchema, String targetSchema, String warehouse) {
		super(typeMapping, workingDatabase, sourceSchema, landingSchema, targetSchema);
		this.warehouse = warehouse == null ? "dummy_wh" : warehouse;
	}

	public SnowTasksMakerVisitor(Map<String, String> typeMapping, String warehouse) {
		this(typeMapping, warehouse, null, null, null, null);
	}

	@Override
	public String visitCreateTable(DDLParser.CreateTableContext ctx) {
		StringBuilder result = new StringBuilder();
		// Extract table namespace and content
		String fqtn = ctx.tableNameSpace().getText();
		initNameSpaces(fqtn);

		String taskFullNS = "";
		String streamFullNS = "";
		String outputTableFullNS = "";
		if ("".equals(this.workingDatabase)) {
			taskFullNS = this.workingDatabase + ".";
			streamFullNS = this.workingDatabase + ".";
			outputTableFullNS = this.workingDatabase + ".";
		}
		taskFullNS += this.landingSchema + "." + this.sourceTableName + "_TASK  WAREHOUSE = " + this.warehouse;
		streamFullNS += this.landingSchema + ".DEBEZIUM_"+this.sourceSchema.toUpperCase()+"_" + this.sourceTableName + "_STRM";
		outputTableFullNS += this.targetSchema + "." + this.sourceTableName;

		String content = visitContent(ctx.content());

//		%s1 = sqlserver_ingest.landing.CUSTOMERS_TASK  WAREHOUSE = wh_ingest
//		%s2 = sqlserver_ingest.landing.DBO_CUSTOMERS_STRM
//      %s3 = sqlserver_ingest.BRZ.CUSTOMERS
//      %s4 = sqlserver_ingest.landing.DBO_CUSTOMERS_STRM
		content = String.format(content, taskFullNS, streamFullNS, outputTableFullNS, streamFullNS);
		result.append(content).append("\r\n\r\n");
		return result.toString();
	}

	@Override
	public String visitFields(DDLParser.FieldsContext ctx) {
		// Build the list of fields
		StringBuilder result = new StringBuilder();

		StringBuilder fields_1 = new StringBuilder("\t");
		StringBuilder partition = new StringBuilder("");
		StringBuilder update = new StringBuilder("");
		StringBuilder insert = new StringBuilder("");
		StringBuilder values = new StringBuilder("");
		StringBuilder idFields = new StringBuilder();
		for (int i = 0; i < ctx.field().size(); i++) {
			String field_1 = visitFieldForFirstSelect(ctx.field(i));
			String field_2 = visitFieldForPartition(ctx.field(i));
			String field_3 = visitFieldForGetName(ctx.field(i));
			String idField = visitFieldForFindingIdField(ctx.field(i));
			if (i > 0) {
				fields_1.append("\r\n\t, ");
				update.append("\t, ");
				insert.append(", ");
				values.append(", ");
			}
			fields_1.append(field_1);
			partition.append(field_2);
			update.append("c.").append(field_3).append(" = s.").append(field_3).append("\r\n");
			insert.append(field_3);
			values.append("s.").append(field_3);
			idFields.append(idField);
		}
		result.append("create task %s\r\n")
			.append("  when system$stream_has_data('%s')\r\n")
			.append("  as\r\n")
			.append("    merge into %s as c\r\n").append("    using (\r\n")
			.append("        select \r\n")
			.append("            t.record_content                                               as record_content\r\n")
			.append("            , t.record_content:payload:op::text                            as op\r\n")
			.append(fields_1).append("\r\n").append("        from %s t\r\n")
			.append("        where t.record_content:payload.op::text in ('u', 'c', 'd')\r\n")
			.append("        -- handle when a batch of records has multiple operations for one record (update, insert, delete)\r\n")
			.append("        -- in this case, take the most recent based on primary key\r\n")
			.append("        qualify row_number() over (\r\n")
			.append("            partition by ").append(partition).append("\r\n")
			.append("            -- combine ts_ms and pos since they may be duplicated in the debezium logs\r\n")
			.append("            order by (t.record_content:payload.ts_ms::integer*100000000000000 + t.record_content:payload.source.ts_ms::integer) desc nulls last\r\n")
			.append("        ) = 1\r\n")
			.append("    ) as s on s.").append(idFields).append(" = c.").append(idFields).append("\r\n")
			.append("    when matched and op = 'd'\r\n")
			.append("        then delete\r\n")
			.append("    -- here we assume that if a record is deleted then recreated immediately, we should handle that as an update operation\r\n")
			.append("    when matched and (op = 'u' or op = 'c')\r\n")
			.append("        then \r\n")
			.append("            update set ").append(update).append("    when not matched and op != 'd'\r\n")
			.append("        then \r\n")
			.append("            insert (").append(insert).append(") \r\n")
			.append("            values (").append(values).append(");");
		return result.toString();
	}

	private String visitFieldForGetName(DDLParser.FieldContext ctx) {
		return visitFieldName(ctx.fieldName());
	}

	public String visitFieldForFindingIdField(DDLParser.FieldContext ctx) {
		// Extract field name and format it
		String fieldName = visitFieldName(ctx.fieldName());
		Field fieldType = null;
		try {
			fieldType = Field.deserialize(visitFieldType(ctx.fieldType()));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (fieldType.id) {
			return fieldName;
		}
		return "";
	}

	public String visitFieldForFirstSelect(DDLParser.FieldContext ctx) {
		// Extract field name and format it
		String fieldName = visitFieldName(ctx.fieldName());
		Field fieldType = null;
		try {
			fieldType = Field.deserialize(visitFieldType(ctx.fieldType()));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (fieldType.id) {
			return String.format("coalesce(t.record_content:payload.before.%s::%s, t.record_content:payload.after.%s::%s) as %s", fieldName, fieldType.type, fieldName, fieldType.type, fieldName);
		} else if (fieldType.size > 0) {
			return String.format("t.record_content:payload.before.%s::%s(%s) as %s", fieldName, fieldType.type, fieldType.size, fieldName);
		} else {
			return String.format("t.record_content:payload.before.%s::%s as %s", fieldName, fieldType.type, fieldName);
		}
	}

	public String visitFieldForPartition(DDLParser.FieldContext ctx) {
		// Extract field name and format it
		String fieldName = visitFieldName(ctx.fieldName());
		Field fieldType = null;
		try {
			fieldType = Field.deserialize(visitFieldType(ctx.fieldType()));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (fieldType.id) {
			return String.format("coalesce(t.record_content:payload.before.%s::%s, t.record_content:payload.after.%s::%s)", fieldName, fieldType.type, fieldName, fieldType.type);
		}
		return "";
	}
}