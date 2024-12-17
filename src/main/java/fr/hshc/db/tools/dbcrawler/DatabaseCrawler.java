package fr.hshc.db.tools.dbcrawler;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class DatabaseCrawler {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseCrawler.class);

	private final Connection connection;
	private final DatabaseConfig config;

	public DatabaseCrawler(Connection connection, DatabaseConfig config) {
		this.connection = connection;
		this.config = config;
	}

	public void crawl() {
		Map<String, Map<String, List<String>>> catalogHierarchy = new LinkedHashMap<>();
		Map<String, Map<String, Map<String, List<String>>>> dbConTypeHierarchy = new LinkedHashMap<>();

		try {
			DatabaseMetaData metaData = connection.getMetaData();
			try (ResultSet catalogs = metaData.getCatalogs()) {
				while (catalogs.next()) {
					String catalog = catalogs.getString("TABLE_CAT");
					Map<String, List<String>> schemaTableMap = new LinkedHashMap<>();

					// Retrieve schemas within the catalog
					try (ResultSet schemas = metaData.getSchemas(catalog, null)) {
						while (schemas.next()) {
							String schema = schemas.getString("TABLE_SCHEM");
							try {
								List<String> tables = fetchTables(catalog, schema);
								schemaTableMap.put(schema, tables);
							} catch (SQLException e) {
								logger.warn("Skipping schema '{}' in catalog '{}' due to access issues: {}", schema,
										catalog, e.getMessage());
							}
						}
					} catch (SQLException e) {
						logger.error("Failed to retrieve Schema information in catalog {}", catalog);
					}

					catalogHierarchy.put(catalog, schemaTableMap);
					dbConTypeHierarchy.put(config.type, catalogHierarchy);
				}
			}
		} catch (SQLException e) {
			logger.error("Failed to retrieve catalog information.", e);
		}

		writeYaml(dbConTypeHierarchy);
	}

	private List<String> fetchTables(String catalog, String schema) throws SQLException {
		List<String> tables = new ArrayList<>();
		DatabaseMetaData metaData = connection.getMetaData();

		try (ResultSet rs = metaData.getTables(catalog, schema, null, new String[] { "TABLE" })) {
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				tables.add(tableName);
			}
		}

		logger.info("Fetched {} tables from schema '{}' in catalog '{}'.", tables.size(), schema, catalog);
		return tables;
	}

	private void writeYaml(Map<String, Map<String, Map<String, List<String>>>> dbConTypeHierarchy) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Yaml yaml = new Yaml(options);

		// Generate a file name based on the database type
		String outputFileName = String.format("output_%s.yaml", config.type.toLowerCase());

		try (FileWriter writer = new FileWriter(outputFileName)) {
			yaml.dump(dbConTypeHierarchy, writer);
			logger.info("Output written to '{}'.", outputFileName);
		} catch (Exception e) {
			logger.error("Failed to write output YAML file '{}'.", outputFileName, e);
		}
	}
}

//
//public class DatabaseCrawler {
//    private static final Logger logger = LoggerFactory.getLogger(DatabaseCrawler.class);
//
//    private final Connection connection;
//    private final DatabaseConfig config;
//
//    public DatabaseCrawler(Connection connection, DatabaseConfig config) {
//        this.connection = connection;
//        this.config = config;
//    }
//
//    public void crawl() {
//        Map<String, List<String>> schemaTableMap = new LinkedHashMap<>();
//
//        for (String schema : config.schemas) {
//            try {
//                List<String> tables = fetchTables(config.database, schema);
//                schemaTableMap.put(schema, tables);
//            } catch (SQLException e) {
//                logger.warn("Skipping schema '{}' due to access issues: {}", schema, e.getMessage());
//            }
//        }
//
//        writeYaml(schemaTableMap);
//    }
//
//    private List<String> fetchTables(String catalog, String schema) throws SQLException {
//        List<String> tables = new ArrayList<>();
//        DatabaseMetaData metaData = connection.getMetaData();
////        try (ResultSet rs = metaData.getCatalogs()) {
////        	while (rs.next()) {
////				String catalog = rs.getString("TABLE_CAT");
////				System.out.println(catalog);
////			}
////        }
////        try (ResultSet rs = metaData.getSchemas()) {
////        	while (rs.next()) {
////				String catalog = rs.getString("TABLE_CATALOG");
////				String schem = rs.getString("TABLE_SCHEM");
////				System.out.println(catalog+"\t"+schem);
////			}
////        }
////        try (ResultSet rs = metaData.getTables("e2i5_dmh_dwh", "dbo", null, null)) {
////        	while (rs.next()) {
////				String catalog = rs.getString("TABLE_CAT");
////				String schem = rs.getString("TABLE_SCHEM");
////				String tbl = rs.getString("TABLE_NAME");
////				System.out.println(catalog+"\t"+schem+"\t"+tbl);
////			}
////        }
//        
//        try (ResultSet rs = metaData.getTables(catalog, schema, null, null)) {
//            while (rs.next()) {
//                String tableName = rs.getString("TABLE_NAME");
//                tables.add(tableName);
//            }
//        }
//
//        logger.info("Fetched {} tables from schema '{}'.", tables.size(), schema);
//        return tables;
//    }
//
//    private void writeYaml(Map<String, List<String>> schemaTableMap) {
//        DumperOptions options = new DumperOptions();
//        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//        Yaml yaml = new Yaml(options);
//
//        try (FileWriter writer = new FileWriter("output.yaml")) {
//            yaml.dump(schemaTableMap, writer);
//            logger.info("Output written to 'output.yaml'.");
//        } catch (Exception e) {
//            logger.error("Failed to write output YAML file.", e);
//        }
//    }
//}
//
//
//
//
////    private List<String> fetchTables(Connection connection, String database, String schema) throws SQLException {
////        connection.getMetaData()
////    	String query = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
////        List<String> tables = new ArrayList<>();
////
////        try (PreparedStatement stmt = connection.prepareStatement(query)) {
////            stmt.setString(1, schema);
////            ResultSet rs = stmt.executeQuery();
////            while (rs.next()) {
////                tables.add(rs.getString("TABLE_NAME"));
////            }
////        }
////
////        return tables;
////    }