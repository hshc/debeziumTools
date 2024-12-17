package fr.hshc.db.tools.dbcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Scanner;

public class DbCrawlerV2 {
    private static final Logger logger = LoggerFactory.getLogger(DbCrawlerV2.class);

    public static void main(String[] args) {
        String workingDir = System.getProperty("user.dir");
        System.out.println("Working Directory = " + workingDir);

        if (args.length < 1) {
            logger.error("Please provide the path to the TOML configuration file.");
            return;
        }

        String tomlFilePath = args[0];
        File tomlFile = new File(tomlFilePath);

        if (!tomlFile.exists()) {
            logger.error("TOML configuration file '{}' not found in the working directory '{}'.", tomlFilePath, workingDir);
            return;
        }

        try {
            // Load all database configurations from TOML
            List<DatabaseConfig> databaseConfigs = DatabaseConfig.loadAll(tomlFilePath);

            if (args.length > 1) {
                // If a YAML file is provided as the second argument, automatically generate DDL
                String yamlFilePath = args[1];
                logger.info("Input YAML file detected. Skipping menu and generating DDL...");
                generateDDL(databaseConfigs, yamlFilePath);
            } else {
                // Otherwise, show the context menu
                try (Scanner scanner = new Scanner(System.in)) {
                    while (true) {
                        // Menu options
                        System.out.println("\nSelect an option:");
                        System.out.println("1. Crawl the database and output schema hierarchy (YAML)");
                        System.out.println("2. Generate DDL for tables in input YAML file");
                        System.out.println("3. Exit");

                        int choice = scanner.nextInt();

                        switch (choice) {
                            case 1:
                                crawlDatabase(config);
                                break;
                            case 2:
                                System.out.print("Enter the path to the input YAML file: ");
                                String yamlFilePath = scanner.next();
                                generateDDL(config, yamlFilePath);
                                break;
                            case 3:
                                System.out.println("Exiting...");
                                return;
                            default:
                                System.out.println("Invalid option. Please try again.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration or execute operation.", e);
        }
    }

    private static void crawlDatabase(DatabaseConfig config) {
        // Open connection for crawling
        try (Connection connection = DriverManager.getConnection(config.url, config.username, config.password)) {
            DatabaseCrawler crawler = new DatabaseCrawler(connection, config);
            crawler.crawl();
        } catch (Exception e) {
            logger.error("Failed to crawl the database.", e);
        }
    }

    private static void generateDDL(List<DatabaseConfig> dbConfigs, String yamlFilePath) {
        DDLGenerator ddlGenerator;
		try {
			ddlGenerator = new DDLGenerator(dbConfigs, yamlFilePath);
	        ddlGenerator.generateDDL();
		} catch (IOException e) {}
		return;
    }
}