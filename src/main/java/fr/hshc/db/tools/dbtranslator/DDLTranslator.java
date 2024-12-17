package fr.hshc.db.tools.dbtranslator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DDLTranslator {
	private static final Logger logger = LoggerFactory.getLogger(DDLTranslator.class);

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println(
					"Usage: java DDLTranslator <inputDDLFile> <mappingCSVFile> <csvSeparator> <outputDDLFile>");
			return;
		}

		String inputDDLFile = args[0];
		String mappingCSVFile = args[1];
		String csvSeparator = args[2];
		String outputDDLFile = args[3];

		try {
			logger.info("Loading type mapping from CSV: {}", mappingCSVFile);
			Map<String, String> typeMapping = loadTypeMapping(mappingCSVFile, csvSeparator);

			logger.info("Translating DDL from file: {} to target file: {}", inputDDLFile, outputDDLFile);
			List<String> translatedDDL = translateDDL(inputDDLFile, typeMapping);

			logger.info("Writing translated DDL to file: {}", outputDDLFile);
			writeToFile(outputDDLFile, translatedDDL);

			logger.info("DDL translation completed successfully.");
		} catch (Exception e) {
			logger.error("Error during DDL translation: ", e);
		}
	}

	/**
	 * Loads the type mapping from a CSV file.
	 *
	 * @param csvFilePath The path to the CSV mapping file.
	 * @param separator   The separator used in the CSV file.
	 * @return A map containing type mappings from DBMS 1 to DBMS 2.
	 * @throws IOException If an error occurs during file reading.
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
	 * Translates the DDL file using the provided type mapping.
	 *
	 * @param ddlFilePath The path to the DDL input file.
	 * @param typeMapping A map containing type mappings.
	 * @return A list of translated DDL lines.
	 * @throws IOException If an error occurs during file reading.
	 */
	private static List<String> translateDDL(String ddlFilePath, Map<String, String> typeMapping) throws IOException {
		List<String> translatedDDL = new ArrayList<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(ddlFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String translatedLine = line;

				// Replace types in the line
				for (Map.Entry<String, String> entry : typeMapping.entrySet()) {
					String dbms1Type = "\\b" + entry.getKey() + "\\b"; // Regex for exact match
					String dbms2Type = entry.getValue();
					translatedLine = translatedLine.replaceAll(dbms1Type, dbms2Type);
				}

				translatedDDL.add(translatedLine);
			}
		}

		return translatedDDL;
	}

	/**
	 * Writes the translated DDL to the output file.
	 *
	 * @param outputFilePath The path to the output file.
	 * @param content        The content to write to the file.
	 * @throws IOException If an error occurs during file writing.
	 */
	private static void writeToFile(String outputFilePath, List<String> content) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
			for (String line : content) {
				writer.write(line);
				writer.newLine();
			}
		}
	}
}

//public class DDLTranslator {
//    private static final Logger logger = LoggerFactory.getLogger(DDLTranslator.class);
//
//    public static void main(String[] args) {
//        if (args.length < 4) {
//            logger.error(Messages.ERROR_USAGE);
//            return;
//        }
//
//        String inputDDLFile = args[0];
//        String typeMappingCSV = args[1];
//        String csvSeparator = args[2]; // CSV separator is now passed as an argument
//        String outputDDLFile = args[3];
//
//        try {
//            // Load type mapping from CSV with specified separator
//            logger.info(Messages.INFO_LOADING_TYPE_MAPPING, typeMappingCSV);
//            Map<String, String> typeMapping = loadTypeMapping(typeMappingCSV, csvSeparator);
//
//            // Translate DDL file
//            logger.info(Messages.INFO_TRANSLATING_DDL, inputDDLFile, outputDDLFile);
//            List<String> translatedDDL = translateDDL(inputDDLFile, typeMapping);
//
//            // Write translated DDL to output file
//            writeToFile(outputDDLFile, translatedDDL);
//            logger.info(Messages.INFO_DDL_TRANSLATION_COMPLETE, outputDDLFile);
//        } catch (Exception e) {
//            logger.error(Messages.ERROR_DDL_TRANSLATION_FAILED, e);
//        }
//    }
//    
//    /**
//     * Loads the type mapping from a CSV file using the specified separator.
//     * @param csvFilePath Path to the CSV file.
//     * @param separator Separator used in the CSV file.
//     * @return A map of type mappings.
//     * @throws IOException If an error occurs while reading the file.
//     */
//    private static Map<String, String> loadTypeMapping(String csvFilePath, String separator) throws IOException {
//        Map<String, String> typeMapping = new HashMap<>();
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.split(separator); // Use the specified separator
//                if (parts.length == 2) {
//                    typeMapping.put(parts[0].trim(), parts[1].trim());
//                }
//            }
//        }
//        return typeMapping;
//    }
//
//    private static List<String> translateDDL(String ddlFilePath, Map<String, String> typeMapping) throws IOException {
//        List<String> translatedDDL = new ArrayList<>();
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(ddlFilePath))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                for (Map.Entry<String, String> entry : typeMapping.entrySet()) {
//                    line = line.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
//                }
//                translatedDDL.add(line);
//            }
//        }
//        return translatedDDL;
//    }
//
//    private static void writeToFile(String outputFilePath, List<String> content) throws IOException {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
//            for (String line : content) {
//                writer.write(line);
//                writer.newLine();
//            }
//        }
//    }
//}
