package fr.hshc.db.tools.dbcrawler;
import java.sql.Connection;
import java.sql.DriverManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbCrawlerV1 {
    private static final Logger logger = LoggerFactory.getLogger(DbCrawlerV1.class);

    public static void main(String[] args) {
    	System.out.println("Working Directory = " + System.getProperty("user.dir"));
    	
        if (args.length < 1) {
            logger.error("Please provide the path to the TOML configuration file.");
            return;
        }

        try {
            // Load configuration from TOML
            DatabaseConfig config = DatabaseConfig.load(args[0]);

            // Connect to the database
            try (Connection connection = DriverManager.getConnection(config.url, config.username, config.password)) {
                DatabaseCrawler crawler = new DatabaseCrawler(connection, config);
                crawler.crawl();
            }
        } catch (Exception e) {
            logger.error("Failed to load configuration or crawl database.", e);
        }
    }
}