package fr.hshc.db.tools.dbcrawler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.hshc.db.tools.Messages;
import fr.hshc.db.tools.dbtranslator.ANTLRDDLTranslator;

/**
 * Classe principale de l'application DbCrawlerV6.
 * Cette application permet :
 * - de scanner une base de données (option `--crawl`),
 * - de générer un script DDL (option `--ddl-gen`),
 * - de traduire un script DDL (option `--ddl-translate`),
 * - d'interagir via un menu (option `--menu`).
 * 
 * Les chemins des fichiers de configuration TOML et des fichiers de mappage DBMS peuvent être stockés
 * dans des variables globales pour éviter des saisies répétées.
 */
public class DbCrawler {
    // Logger pour Afficher les informations, avertissements et erreurs
    private static final Logger logger = LoggerFactory.getLogger(DbCrawler.class);

    // Variable globale pour stocker le chemin du fichier de configuration TOML
    private static String tomlDbConfigFilePath = null;

    // Liste des configurations des bases de données, chargée depuis le fichier TOML
    private static List<DatabaseConfig> databasesConfigs = null;

    // Variable globale pour stocker le chemin du fichier de mappage des types DBMS
    private static String dbmsTypeMappingFilePath = null;
    private static String csvSeparator = null;
    /**
     * Méthode principale de l'application.
     * @param args Arguments de la ligne de commande pour spécifier l'option choisie.
     */
    public static void main(String[] args) {
        // Affiche le répertoire de travail actuel
        String workingDir = System.getProperty("user.dir");
        System.out.println("Répertoire de travail = " + workingDir);

        // Si aucun argument ou l'option "--help" est fourni, afficher l'aide
        if (args.length == 0 || args[0].equalsIgnoreCase("--help")) {
            showHelp();
            return;
        }

        try {
            // Gestion des différentes options en fonction du premier argument
            switch (args[0].toLowerCase()) {
                case "--crawl":
                    if (args.length < 2) {
                        // Erreur si le fichier de configuration TOML n'est pas fourni
                        logger.error(Messages.ERROR_CONFIG_FILE_NOT_PROVIDED);
                        return;
                    }
                    // Charger le fichier de configuration TOML si nécessaire
                    tomlDbConfigFilePath = args[1];
                    databasesConfigs = databasesConfigs == null 
                            ? DatabaseConfig.loadAll(tomlDbConfigFilePath) 
                            : databasesConfigs;
                    // Parcourir les bases de données configurées
                    for (DatabaseConfig config : databasesConfigs) {
                        crawlDatabase(config);
                    }
                    break;

                case "--ddl-gen":
                    if (args.length < 3) {
                        // Erreur si les fichiers TOML et YAML ne sont pas fournis
                        logger.error(Messages.ERROR_CONFIG_FILE_AND_YAML_REQUIRED);
                        return;
                    }
                    // Charger le fichier de configuration TOML
                    tomlDbConfigFilePath = args[1];
                    String yamlTableListFilePath = args[2];
                    databasesConfigs = databasesConfigs == null 
                            ? DatabaseConfig.loadAll(tomlDbConfigFilePath) 
                            : databasesConfigs;
                    // Générer le script DDL
                    generateDDL(databasesConfigs, yamlTableListFilePath);
                    break;

                case "--ddl-translate":
                    if (args.length < 3) {
                        // Erreur si les fichiers nécessaires pour la traduction ne sont pas fournis
                        logger.error(Messages.ERROR_DDL_TRANSLATION_ARGUMENTS_REQUIRED);
                        return;
                    }
                    // Traduire le fichier DDL
                    String inputDDL = args[1];
                    dbmsTypeMappingFilePath = args[2];
                    csvSeparator = ";";
                    tomlDbConfigFilePath = args[3];
                    translateDDL(inputDDL, dbmsTypeMappingFilePath, csvSeparator, null, tomlDbConfigFilePath);
                    break;

                case "--menu":
                    // Lancer le menu interactif
                    launchMenu();
                    break;

                default:
                    // Erreur si l'option fournie est invalide
                    logger.error(Messages.ERROR_INVALID_OPTION, args[0]);
                    showHelp();
            }
        } catch (Exception e) {
            // Gestion des exceptions et erreurs
            logger.error(Messages.ERROR_LOAD_CONF_FAILED, e);
        }
    }

    /**
     * Méthode pour parcourir une base de données en fonction de sa configuration.
     * @param config Configuration de la base de données.
     */
    private static void crawlDatabase(DatabaseConfig config) {
        logger.info(Messages.INFO_DB_CRAWLED, config.type);
        try (Connection connection = DriverManager.getConnection(config.url, config.username, config.password)) {
            DatabaseCrawler crawler = new DatabaseCrawler(connection, config);
            crawler.crawl();
        } catch (Exception e) {
            // Afficher l'erreur si le crawl échoue
            logger.error(Messages.ERROR_DB_CRAWL_FAILED, config.type, e);
        }
    }

    /**
     * Méthode pour générer un script DDL à partir des configurations et d'un fichier YAML.
     * @param dbConfigs Liste des configurations des bases de données.
     * @param yamlFilePath Chemin du fichier YAML contenant la liste des tables.
     */
    private static void generateDDL(List<DatabaseConfig> dbConfigs, String yamlFilePath) {
        try {
            DDLGenerator ddlGenerator = new DDLGenerator(dbConfigs, yamlFilePath);
            ddlGenerator.generateDDL();
        } catch (IOException e) {
            // Afficher l'erreur si la génération échoue
            logger.error(Messages.ERROR_DDL_GEN_FAILED, e);
        }
    }

    /**
     * Méthode pour traduire un script DDL en utilisant un fichier de mappage des types.
     * @param ddlInputFile Chemin du fichier DDL d'entrée.
     * @param typeMappingFile Chemin du fichier de mappage des types DBMS.
     * @param ddlOutputFile Chemin du fichier DDL de sortie.
     * @param tomlDbConfigFilePath 
     */
    private static void translateDDL(String ddlInputFile, String typeMappingFile, String csvSeparator, String ddlOutputFile, String tomlDbConfigFilePath) {
    	if ("".equals(ddlOutputFile) || null == ddlOutputFile) {
    		ddlOutputFile = ddlInputFile.concat(".out");
    	}
        try {
            logger.info(Messages.INFO_TRANSLATING_DDL, ddlInputFile, ddlOutputFile);
            // Pass the separator as an argument to DDLTranslator
            ANTLRDDLTranslator.main(new String[] { 
            		"--inputDDLFile", ddlInputFile, 
            		"--mappingCSVFile", typeMappingFile, 
            		"--csvSeparator", csvSeparator, 
            		"--outputDDLFile", ddlOutputFile, 
            		"--dbConfig", tomlDbConfigFilePath, 
            		"--operation", "7"});
            logger.info(Messages.INFO_DDL_TRANSLATION_COMPLETE, ddlOutputFile);
        } catch (Exception e) {
            logger.error(Messages.ERROR_DDL_TRANSLATION_FAILED, e);
        }
    }

    /**
     * Méthode pour afficher un menu interactif permettant de choisir les options.
     */
    private static void launchMenu() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println(Messages.SELECT_OPTION);
                System.out.println(Messages.CRAWL_CHOICE);
                System.out.println(Messages.GEN_DDL_CHOICE);
                System.out.println(Messages.DDL_TRANSLATOR_CHOICE);
                System.out.println(Messages.EXIT_CHOICE);

                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        if (tomlDbConfigFilePath == null) {
                            System.out.print(Messages.INPUT_TOML_FILE_PATH);
                            tomlDbConfigFilePath = scanner.next();
                            databasesConfigs = DatabaseConfig.loadAll(tomlDbConfigFilePath);
                        }
                        for (DatabaseConfig config : databasesConfigs) {
                            crawlDatabase(config);
                        }
                        break;
                    case 2:
                        if (tomlDbConfigFilePath == null) {
                            System.out.print(Messages.INPUT_TOML_FILE_PATH);
                            tomlDbConfigFilePath = scanner.next();
                            databasesConfigs = DatabaseConfig.loadAll(tomlDbConfigFilePath);
                        }
                        System.out.print(Messages.INPUT_YAML_FILE_PATH);
                        String yamlFilePath = scanner.next();
                        generateDDL(databasesConfigs, yamlFilePath);
                        break;
                    case 3:
                        System.out.print(Messages.INPUT_DDL_FILE_PATH);
                        String ddlInputFile = scanner.next();
                        if (dbmsTypeMappingFilePath == null) {
                            System.out.print(Messages.INPUT_TYPE_MAPPING_FILE_PATH);
                            dbmsTypeMappingFilePath = scanner.next();
                        }
                        if (csvSeparator == null) {
                        	System.out.print(Messages.INPUT_TYPE_CSV_SEPARATOR);
                        	csvSeparator = scanner.next();
                        }
                        translateDDL(ddlInputFile, dbmsTypeMappingFilePath, csvSeparator, ddlInputFile, tomlDbConfigFilePath);
                        break;
                    case 4:
                        System.out.println(Messages.EXITING);
                        return;
                    default:
                        System.out.println(Messages.INVALID_OPTION);
                }
            }
        }
    }

    /**
     * Méthode pour afficher l'aide de l'application.
     */
    private static void showHelp() {
        System.out.println(Messages.HELP_HEADER);
        System.out.println(Messages.HELP_CRAWL);
        System.out.println(Messages.HELP_DDL_GEN);
        System.out.println(Messages.HELP_DDL_TRANSLATE);
        System.out.println(Messages.HELP_MENU);
        System.out.println(Messages.HELP_EXIT);
    }
}
