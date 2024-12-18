/**
 * The ANTLRDDLTranslator class provides functionality to translate DDL (Data Definition Language) files
 * using a custom grammar and type mapping. The output consists of translated DDL statements
 * and related DML (Data Manipulation Language) scripts for target tables, streams, and tasks.
 * This class uses ANTLR for parsing and visitor-based design for custom translation logic.
 */
package fr.hshc.db.tools.dbtranslator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import fr.hshc.db.antlr4.DDLLexer;
import fr.hshc.db.antlr4.DDLParser;
import fr.hshc.db.tools.Messages;
import fr.hshc.db.tools.dbtranslator.infragen.KafkaDbzSrcConnectorMakerVisitor;
import fr.hshc.db.tools.dbtranslator.infragen.SnowTargetTablesBootstrapingDMLMakerVisitor;
import fr.hshc.db.tools.dbtranslator.infragen.SnowTargetTablesDDLMakerVisitor;
import fr.hshc.db.tools.dbtranslator.infragen.KafkaSnowSinkConnectorMakerVisitor;
import fr.hshc.db.tools.dbtranslator.infragen.SnowTargetTablesSnapshotingDMLCTASMakerVisitor;
import fr.hshc.db.tools.dbtranslator.infragen.SnowStreamsForTargetTablesDDLMakerVisitor;
import fr.hshc.db.tools.dbtranslator.infragen.SnowTasksForStreamsMakerVisitor;

public class ANTLRDDLTranslator {
	private ParseTree			tree;			// The ANTLR parse tree representing the DDL file's structure
	private Map<String, String>	typeMapping;	// A mapping of data types for translation
	private String				warehouse;		// The name of the target warehouse
	private String				dbConf;

	/**
	 * Constructs an ANTLRDDLTranslator with the provided type mapping, DDL
	 * input file, and warehouse name.
	 *
	 * @param mappingCSVFile 
	 *            the path to the CSV file containing type mappings
	 * @param csvSeparator
	 *            the separator used in the CSV file
	 * @param inputDDLFile
	 *            the path to the input DDL file
	 * @param warehouse
	 *            the name of the target warehouse
	 * @param dbConf 
	 *            the path to the toml database configuration file
	 * @throws IOException
	 *             if an I/O error occurs during file operations
	 */
	public ANTLRDDLTranslator(String mappingCSVFile, String csvSeparator, String inputDDLFile, String warehouse, String dbConf) throws IOException {
		super();
		this.typeMapping = loadTypeMapping(mappingCSVFile, csvSeparator);
		this.tree = getParserTree(inputDDLFile);
		this.warehouse = warehouse;
		this.dbConf = dbConf;
	}

	/**
	 * The main entry point for the translator application.
	 *
	 * @param args
	 *            the command-line arguments:
	 *            <ul>
	 *            <li>args[0-1]: path to the input DDL file</li>
	 *            <li>args[2-3]: path to the type mapping CSV file</li>
	 *            <li>args[4-5]: CSV separator</li>
	 *            <li>args[6-7]: path to the output file prefix</li>
	 *            </ul>
	 */
	public static void main(String[] args) {
	    if (args.length < 2) {
	        printHelper();
	        return;
	    }

	    // Parse CLI arguments
	    Map<String, String> params = parseArguments(args);

	    // Extract mandatory parameters
	    String inputDDLFile = params.get("inputDDLFile");
	    String mappingCSVFile = params.get("mappingCSVFile");

	    if (inputDDLFile == null || mappingCSVFile == null) {
	        System.err.println("Error: Missing required parameters --inputDDLFile and/or --mappingCSVFile.");
	        printHelper();
	        return;
	    }
//[--csvSeparator <separator>] [--outputDDLFile <prefix>] [--warehouse <name>] [--dbConfig <tomlFilePath>]
	    // Extract optional parameters
	    String csvSeparator = params.getOrDefault("csvSeparator", ";");  // Optional
	    String outputDDLFile = params.getOrDefault("outputDDLFile", ""); // Optional
	    if ("".equals(outputDDLFile)) {
	    	outputDDLFile = inputDDLFile.concat(".out");
	    }
	    String warehouse = params.getOrDefault("warehouse", "$SNOW_WAREHOUSE"); // Default to "dummy_wh"
	    String dbConf = params.get("dbConfig"); // Optional
	    String operation = params.get("operation"); // Optional

	    try (Scanner scanner = new Scanner(System.in)) {
	        ANTLRDDLTranslator ddlTranslator = new ANTLRDDLTranslator(mappingCSVFile, csvSeparator, inputDDLFile, warehouse, dbConf);

	        if (operation == null) {
	            // Interactive mode if no operation is provided
	            System.out.println("Choose an operation to perform:");
	            System.out.println("1: Generate Target Tables DDL");
	            System.out.println("2: Generate Bootstrapping DML");
	            System.out.println("3: Generate Snapshoting DML");
	            System.out.println("4: Generate Target Table Provisioning Streams");
	            System.out.println("5: Generate Stream-Provisioned Table Tasks");
	            System.out.println("6: Generate Debezium/Snowflake Kafka Source/Sink configurations");
	            System.out.println("7: All the 6");
	            System.out.print("Enter your choice (1-7): ");
	            operation = scanner.nextLine();
	        }

	        // Perform the selected operation
	        String result = null;
	        switch (operation) {
	            case "1":
	                result = ddlTranslator.computeTargetTablesDDL();
                    writeToFile(outputDDLFile + ".SnowTargetTablesDDL", result);
	                System.out.println("Target Tables DDL translation completed.");
	                break;
	            case "2":
	                result = ddlTranslator.computeTargetTablesBoostrapingDML();
                    writeToFile(outputDDLFile + ".SnowTargetTablesBootsrapingDML", result);
	                System.out.println("Target Tables Bootstrapping DML translation completed.");
	                break;
	            case "3":
	                result = ddlTranslator.computeTargetTablesSnapshotingDML();
                    writeToFile(outputDDLFile + ".SnowTargetTablesSnapshotingDML", result);
	                System.out.println("Target Tables Snapshoting DML translation completed.");
	                break;
	            case "4":
	                result = ddlTranslator.computeStreamsForTargetTablesDDL();
                    writeToFile(outputDDLFile + ".SnowTargetTableProvisionningStreams", result);
	                System.out.println("Target Table Provisioning Streams generation completed.");
	                break;
	            case "5":
	                result = ddlTranslator.computeTasksForStreams();
                    writeToFile(outputDDLFile + ".SnowTasksForStreams", result);
	                System.out.println("Stream-Provisioned Target Table Tasks generation completed.");
	                break;
	            case "6":
                    if (dbConf == null) {
                        System.out.print(Messages.INPUT_TOML_FILE_PATH);
                        dbConf = scanner.next();
                    }
                    result = ddlTranslator.computeKafkaDbzSrcConfig();
                    writeToFile(outputDDLFile + ".KafkaDbzSrcConfig", result);
	                System.out.println("Debezium Kafka Source Connector configuration generation completed.");
                    
                    result = ddlTranslator.computeKafkaSnowSinkConfig();
                    writeToFile(outputDDLFile + ".KafkaSnowSinkConfig", result);
	                System.out.println("Snowflake Kafka Sink Connector configuration generation completed.");
	                break;
	            case "7":
                    result = ddlTranslator.computeKafkaDbzSrcConfig();
                    writeToFile(outputDDLFile + ".KafkaDbzSrcConfig", result);
                    
                    result = ddlTranslator.computeKafkaSnowSinkConfig();
                    writeToFile(outputDDLFile + ".KafkaSnowSinkConfig", result);
                    
	                result = ddlTranslator.computeTargetTablesDDL();
                    writeToFile(outputDDLFile + ".SnowTargetTablesDDL", result);
                    
	                result = ddlTranslator.computeTargetTablesBoostrapingDML();
                    writeToFile(outputDDLFile + ".SnowTargetTablesBootsrapingDML", result);
                    
	                result = ddlTranslator.computeTargetTablesSnapshotingDML();
                    writeToFile(outputDDLFile + ".SnowTargetTablesSnapshotingDML", result);
                    
	                result = ddlTranslator.computeStreamsForTargetTablesDDL();
                    writeToFile(outputDDLFile + ".SnowStreamsForTargetTablesDDL", result);
                    
	                result = ddlTranslator.computeTasksForStreams();
                    writeToFile(outputDDLFile + ".SnowTasksForStreams", result);
                    if (dbConf == null) {
                        System.out.print(Messages.INPUT_TOML_FILE_PATH);
                        dbConf = scanner.next();
                    }
	                break;
	            default:
	                System.err.println("Invalid operation. Please choose a number between 1 and 7.");
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	private String computeKafkaDbzSrcConfig() {
		KafkaDbzSrcConnectorMakerVisitor visitor = new KafkaDbzSrcConnectorMakerVisitor(typeMapping, this.dbConf);
		return visitor.visit(this.tree);
	}

	/**
	 * Parses CLI arguments in the form of --paramName value.
	 */
	private static Map<String, String> parseArguments(String[] args) {
	    Map<String, String> params = new HashMap<>();
	    for (int i = 0; i < args.length - 1; i += 2) {
	        if (args[i].startsWith("--")) {
	            String paramName = args[i].substring(2); // Remove the "--" prefix
	            String paramValue = args[i + 1];
	            params.put(paramName, paramValue);
	        }
	    }
	    return params;
	}

	/**
	 * Prints the helper message for the CLI usage.
	 */
	private static void printHelper() {
	    System.out.println("Usage: java ANTLRDDLTranslator --inputDDLFile <file> --mappingCSVFile <file> [--csvSeparator <separator>] [--outputDDLFile <prefix>] [--warehouse <name>] [--dbConfig <tomlFilePath>] [--operation <number>]");
	    System.out.println();
	    System.out.println("Mandatory arguments:");
	    System.out.println("  --inputDDLFile    Path to the input DDL file.");
	    System.out.println("  --mappingCSVFile  Path to the CSV file containing type mappings.");
	    System.out.println();
	    System.out.println("Optional arguments:");
	    System.out.println("  --csvSeparator    Separator used in the CSV file (default: ',').");
	    System.out.println("  --outputDDLFile   Prefix for the output files (if omitted, no files are generated).");
	    System.out.println("  --warehouse       Name of the target warehouse (default: 'dummy_wh').");
	    System.out.println("  --dbConfig        Path to TOML database connector configuration");
	    System.out.println("  --operation       Operation to perform:");
	    System.out.println("                    1: Translate Target Tables DDL");
	    System.out.println("                    2: Translate Bootstrapping DML");
	    System.out.println("                    3: Translate Snapshoting DML");
	    System.out.println("                    4: Generate Target Table Provisioning Streams");
	    System.out.println("                    5: Generate Stream-Provisioned Table Tasks");
	    System.out.println("                    6: Generate Debezium Kafka Source and Snowflake Kafka Sink connectors configurations");
	    System.out.println("                    7: All the 6");
	    System.out.println("If --operation is not specified, the program will prompt for it interactively.");
	}
	
	/**
	 * Loads the type mapping from a CSV file.
	 *
	 * @param csvFilePath
	 *            the path to the CSV file
	 * @param separator
	 *            the separator used in the CSV file
	 * @return a map of source types to target types
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private static Map<String, String> loadTypeMapping(String csvFilePath, String separator) throws IOException {
		Map<String, String> typeMapping = new HashMap<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(separator);
				if (parts.length == 2) {
					typeMapping.put(parts[0].trim().toUpperCase(), parts[1].trim());
				}
			}
		}
		return typeMapping;
	}

	/**
	 * Computes the DDL for target tables using the type mapping.
	 *
	 * @return the translated DDL for target tables
	 * @throws IOException
	 *             if an error occurs during processing
	 */
	private String computeTargetTablesDDL() throws IOException {
		SnowTargetTablesDDLMakerVisitor visitor = new SnowTargetTablesDDLMakerVisitor(typeMapping, this.dbConf);
		return visitor.visit(this.tree);
	}

	/**
	 * Computes the DML for bootstrapping target tables.
	 *
	 * @return the translated DML for bootstrapping target tables
	 * @throws IOException
	 *             if an error occurs during processing
	 */
	private String computeTargetTablesBoostrapingDML() throws IOException {
		SnowTargetTablesBootstrapingDMLMakerVisitor visitor = new SnowTargetTablesBootstrapingDMLMakerVisitor(typeMapping);
		return visitor.visit(this.tree);
	}

	/**
	 * Computes the DML for provisioning streams for target tables.
	 *
	 * @return the translated DML for provisioning streams
	 * @throws IOException
	 *             if an error occurs during processing
	 */
	private String computeStreamsForTargetTablesDDL() throws IOException {
		SnowStreamsForTargetTablesDDLMakerVisitor visitor = new SnowStreamsForTargetTablesDDLMakerVisitor();
		return visitor.visit(this.tree);
	}

	/**
	 * Computes the DML for snapshotting target tables.
	 *
	 * @return the translated DML for snapshotting target tables
	 * @throws IOException
	 *             if an error occurs during processing
	 */
	private String computeTargetTablesSnapshotingDML() throws IOException {
		SnowTargetTablesSnapshotingDMLCTASMakerVisitor visitor = new SnowTargetTablesSnapshotingDMLCTASMakerVisitor(typeMapping);
		return visitor.visit(this.tree);
	}


	private String computeKafkaSnowSinkConfig() throws IOException {
		KafkaSnowSinkConnectorMakerVisitor visitor = new KafkaSnowSinkConnectorMakerVisitor(typeMapping, this.dbConf);
		return visitor.visit(this.tree);
	}

	/**
	 * Computes the tasks for stream-provisioned target tables.
	 *
	 * @return the translated tasks
	 * @throws IOException
	 *             if an error occurs during processing
	 */
	private String computeTasksForStreams() throws IOException {
		SnowTasksForStreamsMakerVisitor visitor = new SnowTasksForStreamsMakerVisitor(typeMapping, this.warehouse);
		return visitor.visit(this.tree);
	}

	/**
	 * Parses the input DDL file to produce a parse tree using ANTLR.
	 *
	 * @param inputDDLFile
	 *            the path to the input DDL file
	 * @return the parse tree representing the DDL file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private static ParseTree getParserTree(String inputDDLFile) throws IOException {
		CharStream input = CharStreams.fromFileName(inputDDLFile);
		DDLLexer lexer = new DDLLexer(input); // Lexer for the DDL grammar
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		DDLParser parser = new DDLParser(tokens); // Parser for the DDL grammar

		return parser.ddlFile(); // Entry point for parsing the DDL file
	}

	/**
	 * Writes content to a specified file.
	 *
	 * @param outputFilePath
	 *            the path to the output file
	 * @param content
	 *            the content to write
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private static void writeToFile(String outputFilePath, String content) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
			writer.write(content);
		}
	}
}
