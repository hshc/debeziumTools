package fr.hshc.db.tools.dbtranslator.infragen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.hshc.db.antlr4.DDLParser.CreateTableContext;
import fr.hshc.db.antlr4.DDLParser.FieldSizeContext;
import fr.hshc.db.antlr4.DDLParser.FieldTypeContext;
import fr.hshc.db.antlr4.DDLParser.FiledIdentContext;
import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.antlr4.DDLParserBaseVisitor;

abstract class SnowCodeGeneratorGenericVisitor extends DDLParserBaseVisitor<String> {

	protected final Map<String, String>	typeMapping;
	protected String					sourceSchema	= null;
	protected String					landingSchema	= null;
	protected String					targetSchema	= null;
	protected String					workingDatabase	= null;
	protected String					sourceTableName	= "";

	SnowCodeGeneratorGenericVisitor(Map<String, String> typeMapping, String workingDatabase, String sourceSchema, String landingSchema, String targetSchema) {
		super();
		this.typeMapping = typeMapping;
		this.workingDatabase = workingDatabase == null ? "" : workingDatabase;
		this.sourceSchema = sourceSchema == null ? "defaultSchema" : sourceSchema;
		this.landingSchema = landingSchema == null ? "landing" : landingSchema;
		this.targetSchema = targetSchema == null ? "" : targetSchema;
	}

	SnowCodeGeneratorGenericVisitor(Map<String, String> typeMapping) {
		this(typeMapping, null, null, null, null);
	}

	protected void initNameSpaces(String inputTableNameSpace) {
		String[] result = inputTableNameSpace.split("\\.");
		int length = result.length;
		this.sourceTableName = result[length - 1];
		if (length > 1 && "defaultSchema".equals(sourceSchema)) {
			this.sourceSchema = result[length - 2];
		}
		if (length > 2 && "".equals(workingDatabase)) {
			workingDatabase = result[length - 3];
		}
		if ("".equals(targetSchema)) {
			targetSchema = sourceSchema;
		}
	}

	@Override
	public String visitDdlFile(DDLParser.DdlFileContext ctx) {
		// Visit all CREATE TABLE statements and join their transformations
		List<CreateTableContext> lst = ctx.createTable();
		String result = lst
					.stream()
					.map(this::visitCreateTable)
					.collect(Collectors.joining("\n"));
		System.out.println(result);
		return result.toString();
	}

	@Override
	public String visitContent(DDLParser.ContentContext ctx) {
		// Visit fields to build the SELECT columns
		return visitFields(ctx.fields());
	}

	@Override
	public String visitFields(DDLParser.FieldsContext ctx) {
		// Build the list of fields
		StringBuilder fields = new StringBuilder();
		for (int i = 0; i < ctx.field().size(); i++) {
			String field = visitField(ctx.field(i));
			if (i > 0) {
				fields.append(",\r\n");
			}
			fields.append("\t").append(field);
		}
		return fields.toString();
	}

	@Override
	public String visitFieldName(DDLParser.FieldNameContext ctx) {
		// Get the full field name
		return ctx.getText();
	}
	
	@Override
	public String visitFieldType(FieldTypeContext ctx) {
		Field field = null;
		String dbms1Type = ctx.children.getFirst().toString().toUpperCase();
		String dbms2Type = null;
		FiledIdentContext ident = null;
		FieldSizeContext size = null;
		int iSize = 0;
		int i = ctx.getChildCount();
		if (i > 1) {
			ident = ctx.filedIdent();
			size = ctx.fieldSize();
		}

		if (typeMapping.containsKey(dbms1Type)) {
			dbms2Type = typeMapping.get(dbms1Type);
		}
		if (dbms2Type == null) {
			dbms2Type = dbms1Type;
		}
		if (size != null && dbms2Type.endsWith("()")) {
			iSize = Integer.parseInt(size.getText().substring(1, size.getText().length() - 1));
			dbms2Type = dbms2Type.substring(0, dbms2Type.indexOf("()"));
		}
		field = new Field(dbms2Type, iSize, ident != null);
		try {
			return field.serialize();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static class Field implements Serializable {
		private static final long serialVersionUID = 7477404464954942327L;
	
		public Field(final String type, final int size, final boolean id) {
			super();
			this.type = type;
			this.size = size;
			this.id = id;
		}
		public final String type;
		public final int size;
		public final boolean id;
	
	    // Serialize this object to a String
	    public String serialize() throws IOException {
	        // Convert the object into a byte array
	        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	             ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
	            objectStream.writeObject(this);  // Serialize the object
	            byte[] byteArray = byteStream.toByteArray();
	            
	            // Convert byte array to a Base64 string
	            return Base64.getEncoder().encodeToString(byteArray);
	        }
	    }
	
	    // Deserialize a String back into an object
	    static Field deserialize(String str) throws IOException, ClassNotFoundException {
	        // Decode the Base64 string into a byte array
	        byte[] data = Base64.getDecoder().decode(str);
	        
	        // Convert the byte array back into an object
	        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
	             ObjectInputStream objectStream = new ObjectInputStream(byteStream)) {
	            return (Field) objectStream.readObject();  // Deserialize the object
	        }
	    }
	}
}