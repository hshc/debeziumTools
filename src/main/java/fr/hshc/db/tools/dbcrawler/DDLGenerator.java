package fr.hshc.db.tools.dbcrawler;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import fr.hshc.db.tools.Messages;

public class DDLGenerator {
	private static final Logger logger = LoggerFactory.getLogger(DDLGenerator.class);

    private DatabaseConfig config;
    private Map<String, Map<String, List<String>>> catalogHierarchy;

    public DDLGenerator(List<DatabaseConfig> dbConfigs, String inputYamlFile) throws IOException {
        Map<String, Map<String, Map<String, List<String>>>> dbConTypeHierarchy;
        String dbType = null;
        try (FileReader reader = new FileReader(inputYamlFile)) {
        	dbConTypeHierarchy = new Yaml().load(reader);
        	for (Entry<String, Map<String, Map<String, List<String>>>> dbConType : dbConTypeHierarchy.entrySet()) {
        		dbType = dbConType.getKey();
        		this.catalogHierarchy = dbConType.getValue();
        		break;
        	}
            for (DatabaseConfig config : dbConfigs) {
            	if (config.type.equals(dbType)) {
            		this.config = config;
            	}
            }
        } catch (IOException ie) {
        	logger.error(Messages.ERROR_YAML_LOADING_FAILED, inputYamlFile, System.getProperty("user.dir"), ie);
        	throw ie;
		}
    }

    public void generateDDL() {
    	logger.info(Messages.INFO_DDL_GEN, this.config.type);
        String outputFileName = this.config.type + "_ddls.sql"; // Use database type for output file naming
        

        // Open connection for DDL generation
        try (Connection connection = DriverManager.getConnection(this.config.url, this.config.username, this.config.password)) {
            // Prepare output file for DDL statements
            try (FileWriter writer = new FileWriter(outputFileName)) {
                for (Map.Entry<String, Map<String, List<String>>> catalogEntry : catalogHierarchy.entrySet()) {
                    String catalog = catalogEntry.getKey();

                    for (Map.Entry<String, List<String>> schemaEntry : catalogEntry.getValue().entrySet()) {
                        String schema = schemaEntry.getKey();

                        for (String table : schemaEntry.getValue()) {
                            String ddl = fetchDDL(connection, catalog, schema, table);
                            if (ddl != null) {
                                writer.write(ddl + ";\n\n");
                            }
                        }
                    }
                }
                logger.info(Messages.INFO_DDL_OUTPUT_FILE, outputFileName);
            } catch (Exception e) {
            	logger.error(Messages.ERROR_DDL_GEN_FAILED, e);
            }
        } catch (Exception e) {
            logger.error(Messages.ERROR_DDL_GEN_FAILED_FOR_DB, this.config.type, e);
        }
        

    }

    private String fetchDDL(Connection connection, String catalog, String schema, String table) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Attempt to fetch table DDL
            try (ResultSet rs = metaData.getColumns(catalog, schema, table, null)) {
                StringBuilder ddlBuilder = new StringBuilder();
                ddlBuilder.append("CREATE TABLE ").append(catalog).append(".").append(schema).append(".").append(table).append(" (\n");

                boolean first = true;
                while (rs.next()) {
                    if (!first) {
                        ddlBuilder.append(",\n");
                    }
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("TYPE_NAME");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    ddlBuilder.append("  ").append(columnName).append(" ").append(dataType);

                    // Add column size if applicable
                    if (columnSize > 0) {
                        ddlBuilder.append("(").append(columnSize).append(")");
                    }
                    first = false;
                }

                ddlBuilder.append("\n)");
                return ddlBuilder.toString();
            }
        } catch (Exception e) {
            logger.warn(Messages.ERROR_DDL_FETCHING_FAILED, catalog, schema, table, e);
        }
        return null;
    }
}