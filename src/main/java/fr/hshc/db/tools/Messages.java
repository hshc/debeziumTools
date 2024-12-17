package fr.hshc.db.tools;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "fr.hshc.db.tools.messages";
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());

    public static final String ERROR_DDL_GEN_FAILED_FOR_DB = Messages.getString("ERROR_DDL_GEN_FAILED_FOR_DB");
    public static final String ERROR_DDL_GEN_FAILED = Messages.getString("ERROR_DDL_GEN_FAILED");
    public static final String INFO_DDL_OUTPUT_FILE = Messages.getString("INFO_DDL_OUTPUT_FILE");
    public static final String INFO_DDL_GEN = Messages.getString("INFO_DDL_GEN");
    public static final String ERROR_YAML_LOADING_FAILED = Messages.getString("ERROR_YAML_LOADING_FAILED");
	public static final String ERROR_DDL_FETCHING_FAILED = Messages.getString("ERROR_DDL_FETCHING_FAILED");
    public static final String ERROR_DB_CRAWL_FAILED = Messages.getString("ERROR_DB_CRAWL_FAILED");
	public static final String INFO_DB_CRAWLED = Messages.getString("INFO_DB_CRAWLED");
	public static final String ERROR_LOAD_CONF_FAILED = Messages.getString("ERROR_LOAD_CONF_FAILED");
	public static final String INVALID_OPTION = Messages.getString("INVALID_OPTION");
	public static final String EXITING = Messages.getString("EXITING");
	public static final String INPUT_YAML_FILE_PATH = Messages.getString("INPUT_YAML_FILE_PATH");
	public static final String EXIT_CHOICE = Messages.getString("EXIT_CHOICE");
	public static final String GEN_DDL_CHOICE = Messages.getString("GEN_DDL_CHOICE");
	public static final String CRAWL_CHOICE = Messages.getString("CRAWL_CHOICE");
	public static final String SELECT_OPTION = Messages.getString("SELECT_OPTION");
	public static final String INFO_SKIP_MENU = Messages.getString("INFO_SKIP_MENU");
	public static final String ERROR_FILE_NOT_FOUND = Messages.getString("ERROR_FILE_NOT_FOUND");
	public static final String ERROR_CONFIG_FILE_NOT_PROVIDED = Messages.getString("ERROR_CONFIG_FILE_NOT_PROVIDED");
	public static final String INFO_LOADING_TYPE_MAPPING= Messages.getString("INFO_LOADING_TYPE_MAPPING");
	
	public static final String DDL_TRANSLATOR_CHOICE         = Messages.getString("DDL_TRANSLATOR_CHOICE");
	public static final String INPUT_DDL_FILE_PATH           = Messages.getString("INPUT_DDL_FILE_PATH");
	public static final String INPUT_TYPE_MAPPING_FILE_PATH  = Messages.getString("INPUT_TYPE_MAPPING_FILE_PATH");
	public static final String INPUT_OUTPUT_DDL_FILE_PATH    = Messages.getString("INPUT_OUTPUT_DDL_FILE_PATH");
	public static final String INFO_TRANSLATING_DDL          = Messages.getString("INFO_TRANSLATING_DDL");
	public static final String INFO_DDL_TRANSLATION_COMPLETE = Messages.getString("INFO_DDL_TRANSLATION_COMPLETE");
	public static final String ERROR_DDL_TRANSLATION_FAILED  = Messages.getString("ERROR_DDL_TRANSLATION_FAILED");
	public static final String ERROR_USAGE                   = Messages.getString("ERROR_USAGE");
	public static final String ERROR_CONFIG_FILE_AND_YAML_REQUIRED      = Messages.getString("ERROR_CONFIG_FILE_AND_YAML_REQUIRED");
	public static final String ERROR_DDL_TRANSLATION_ARGUMENTS_REQUIRED = Messages.getString("ERROR_DDL_TRANSLATION_ARGUMENTS_REQUIRED");
	public static final String ERROR_INVALID_OPTION                     = Messages.getString("ERROR_INVALID_OPTION");
	public static final String INPUT_TOML_FILE_PATH                     = Messages.getString("INPUT_TOML_FILE_PATH");
	public static final String HELP_HEADER                              = Messages.getString("HELP_HEADER");
	public static final String HELP_CRAWL                               = Messages.getString("HELP_CRAWL");
	public static final String HELP_DDL_GEN                             = Messages.getString("HELP_DDL_GEN");
	public static final String HELP_DDL_TRANSLATE                       = Messages.getString("HELP_DDL_TRANSLATE");
	public static final String HELP_MENU                                = Messages.getString("HELP_MENU");
	public static final String HELP_EXIT                                = Messages.getString("HELP_EXIT");
	public static final String INPUT_TYPE_CSV_SEPARATOR					= Messages.getString("INPUT_TYPE_CSV_SEPARATOR");
	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
