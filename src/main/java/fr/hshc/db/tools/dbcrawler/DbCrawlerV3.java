package fr.hshc.db.tools.dbcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.hshc.db.tools.Messages;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Scanner;

public class DbCrawlerV3 {
	private static final Logger logger = LoggerFactory.getLogger(DbCrawlerV3.class);

    public static void main(String[] args) {
        String workingDir = System.getProperty("user.dir");
        System.out.println("Working Directory = " + workingDir);

        if (args.length < 1) {
            logger.error(Messages.ERROR_CONFIG_FILE_NOT_PROVIDED);
            return;
        }

        String tomlFilePath = args[0];
        File tomlFile = new File(tomlFilePath);

        if (!tomlFile.exists()) {
            logger.error(Messages.ERROR_FILE_NOT_FOUND, tomlFilePath, workingDir);
            return;
        }

        try {
            // Load all database configurations from TOML
            List<DatabaseConfig> databaseConfigs = DatabaseConfig.loadAll(tomlFilePath);

            if (args.length > 1) {
                // If a YAML file is provided as the second argument, automatically generate DDL
                String yamlFilePath = args[1];
                logger.info(Messages.INFO_SKIP_MENU);
                generateDDL(databaseConfigs, yamlFilePath);
            } else {
                // Otherwise, show the context menu
                try (Scanner scanner = new Scanner(System.in)) {
                    while (true) {
                        // Menu options
                        System.out.println(Messages.SELECT_OPTION);
                        System.out.println(Messages.CRAWL_CHOICE);
                        System.out.println(Messages.GEN_DDL_CHOICE);
                        System.out.println(Messages.EXIT_CHOICE);

                        int choice = scanner.nextInt();

                        switch (choice) {
                            case 1:
                                for (DatabaseConfig config : databaseConfigs) {
                                    crawlDatabase(config);
                                }
                                break;
                            case 2:
                                System.out.print(Messages.INPUT_YAML_FILE_PATH);
                                String yamlFilePath = scanner.next();
                                generateDDL(databaseConfigs, yamlFilePath);
                                break;
                            case 3:
                                System.out.println(Messages.EXITING);
                                return;
                            default:
                                System.out.println(Messages.INVALID_OPTION);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(Messages.ERROR_LOAD_CONF_FAILED, e);
        }
    }

    private static void crawlDatabase(DatabaseConfig config) {
        logger.info(Messages.INFO_DB_CRAWLED, config.type);
        // Open connection for crawling
        try (Connection connection = DriverManager.getConnection(config.url, config.username, config.password)) {
            DatabaseCrawler crawler = new DatabaseCrawler(connection, config);
            crawler.crawl();
        } catch (Exception e) {
            logger.error(Messages.ERROR_DB_CRAWL_FAILED, config.type, e);
        }
    }

    private static void generateDDL(List<DatabaseConfig> dbConfigs, String yamlFilePath) {
        DDLGenerator ddlGenerator;
		try {
			ddlGenerator = new DDLGenerator(dbConfigs, yamlFilePath);
		    ddlGenerator.generateDDL();
		} catch (IOException e) {}
    }
}
