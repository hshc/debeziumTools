package fr.hshc.db.tools.dbcrawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.moandjiezana.toml.Toml;

public class DatabaseConfig {
	public String type; // Type of the database (e.g., "sqlserver", "snowflake")
	public String url; // Connection URL
	public String username; // Database username
	public String password; // Database password
	public String database; // Database name
	public String server; // DB Server name
	public List<String> schemas; // List of schemas to crawl

	/**
	 * Loads all database configurations from a TOML file.
	 *
	 * @param filePath Path to the TOML configuration file.
	 * @return List of DatabaseConfig objects.
	 */
	public static List<DatabaseConfig> loadAll(String filePath) {
		Toml toml = new Toml().read(new File(filePath));
		List<DatabaseConfig> configs = new ArrayList<>();

		List<Toml> databases = toml.getTables("database");
		if (databases == null || databases.isEmpty()) {
			throw new IllegalArgumentException("No database configurations found in the TOML file.");
		}

		for (Toml dbToml : databases) {
			DatabaseConfig config = new DatabaseConfig();
			config.type = dbToml.getString("type");
			config.url = dbToml.getString("url");
			config.username = dbToml.getString("username");
			config.password = dbToml.getString("password");
			config.database = dbToml.getString("database");
			config.schemas = dbToml.getList("schemas", new ArrayList<>());

			// Validate required fields
			if (config.type == null || config.url == null || config.username == null || config.password == null) {
				throw new IllegalArgumentException("Missing required database configuration fields in the TOML file.");
			}

			config.server = config.url.split("://")[1].split(":")[0].split("\\.")[0];
			configs.add(config);
		}

		return configs;
	}

	public static DatabaseConfig load(String filePath) throws Exception {
//		String content = new String(Files.readAllBytes(Paths.get(filePath)));
//		Toml toml = new Toml().read(content);
		Toml toml = new Toml().read(new File(filePath));
		return toml.to(DatabaseConfig.class);
	}
}