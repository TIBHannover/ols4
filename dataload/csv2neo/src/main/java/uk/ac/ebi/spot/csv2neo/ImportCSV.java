package uk.ac.ebi.spot.csv2neo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.neo4j.driver.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import static uk.ac.ebi.spot.csv2neo.QueryGeneration.*;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
public class ImportCSV {

    static FileReader fr;
    static BufferedReader br;

    public static List<File> listFiles(File[] files) throws IOException {
        List<File> fileList = new ArrayList<File>();
        for (File file : files) {
            if (file.isDirectory()) {
                fileList.addAll(listFiles(file.listFiles()));
            } else {
                fileList.add(file);
            }
        }

        return fileList;
    }

    public static void executeBatchedNodeQueries(List<File> files, Session session, int batchSize) throws IOException {
        for (File file : files) {
            if (!(file.getName().contains("_ontologies") || file.getName().contains("_properties")
                    || file.getName().contains("_individuals") || file.getName().contains("_classes")) || !file.getName().endsWith(".csv"))
                continue;
            Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
            org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
            String[] headers = csvParser.getHeaderNames().toArray(String[]::new);
            List<List<CSVRecord>> splitRecords = splitList(csvParser.getRecords(),batchSize);
            for (List<CSVRecord> records : splitRecords){
                try (Transaction tx = session.beginTransaction()){
                    for (CSVRecord csvRecord : records) {
                        String[] row = csvRecord.toList().toArray(String[]::new);
                        String query = generateBlankNodeCreationQuery(headers,row);
                        Map<String,Object> params = generateProps(headers,row);
                        if(query.isEmpty())
                            System.out.println("empty query for appended line: "+Arrays.toString(row)+" in file: "+file);
                        else
                            tx.run(query,params);
                    }
                    tx.commit();
                }
            }
        }
    }

    public static void executeBatchedRelationshipQueries(List<File> files, Session session, int batchSize) throws IOException {
        for (File file : files) {
            if ((!file.getName().contains("_edges")) || !file.getName().endsWith(".csv"))
                continue;

            Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
            org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
            String[] headers = csvParser.getHeaderNames().toArray(String[]::new);
            List<List<CSVRecord>> splitRecords = splitList(csvParser.getRecords(), batchSize);
            for (List<CSVRecord> records : splitRecords){
                try (Transaction tx = session.beginTransaction()){
                    for (CSVRecord csvRecord : records) {
                        String[] row = csvRecord.toList().toArray(String[]::new);
                        String query = generateRelationCreationQuery(headers,row);
                        if(query.isEmpty())
                            System.out.println("empty query for appended line: "+Arrays.toString(row)+" in file: "+file);
                        else
                            tx.run(query);
                    }
                    tx.commit();
                }
            }
        }
    }

    public static <T> List<List<T>> splitList(List<T> list, int batchSize) {
        List<List<T>> subLists = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            subLists.add(new ArrayList<>(list.subList(i, Math.min(i + batchSize, list.size()))));
        }
        return subLists;
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption("i", "ingest",false, "ingest ontologies");
        options.addOption("rm", "remove",true, "remove ontology by commas");
        options.addOption("a", "authenticated",false, "use authentication");
        options.addOption("u", "user",true, "neo4j user name");
        options.addOption("pw", "password",true, "neo4j user password");
        options.addOption("uri", "databaseuri",true, "neo4j database uri");
        options.addOption("db", "database",true, "neo4j database name");
        options.addOption("d", "directory",true, "neo4j csv import directory");
        options.addOption("bs", "batchsize",true, "batch size for splitting queries into multiple transactions.");
        return options;
    }

    public static void main(String... args) throws IOException, ParseException {
        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);
        final String db = cmd.hasOption("db") ? cmd.getOptionValue("db") : "neo4j";
        final String dbUri = cmd.hasOption("uri") ? cmd.getOptionValue("uri") : "neo4j://localhost";
        final String dbUser = cmd.hasOption("u") ? cmd.getOptionValue("u") : "neo4j";
        final String dbPassword = cmd.hasOption("pw") ? cmd.getOptionValue("pw") : "testtest";
        final String directory = cmd.hasOption("d") ? cmd.getOptionValue("d") : "/tmp/out";
        final String ontologiesToBeRemoved = cmd.hasOption("rm") ? cmd.getOptionValue("rm") : "";
        final int batchSize = cmd.hasOption("bs") && Integer.parseInt(cmd.getOptionValue("bs"))>0 ? Integer.parseInt(cmd.getOptionValue("bs")) : 1000;

        try (var driver = cmd.hasOption("a") ? GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword)) : GraphDatabase.driver(dbUri)) {
            driver.verifyConnectivity();
            try (var session = driver.session(SessionConfig.builder().withDatabase(db).build())) {
                List<String> indexCommands = new ArrayList<>();
                indexCommands.add("CREATE CONSTRAINT IF NOT EXISTS FOR (n:Ontology) REQUIRE n.id IS UNIQUE");
                indexCommands.add("CREATE CONSTRAINT IF NOT EXISTS FOR (n:OntologyEntity) REQUIRE n.id IS UNIQUE");
                indexCommands.add("CREATE CONSTRAINT IF NOT EXISTS FOR (n:OntologyClass) REQUIRE n.id IS UNIQUE");
                indexCommands.add("CREATE CONSTRAINT IF NOT EXISTS FOR (n:OntologyProperty) REQUIRE n.id IS UNIQUE");
                indexCommands.add("CREATE CONSTRAINT IF NOT EXISTS FOR (n:OntologyIndividual) REQUIRE n.id IS UNIQUE");
                indexCommands.add("CREATE TEXT INDEX ontology_id IF NOT EXISTS FOR (n:Ontology) ON (n.id)");
                indexCommands.add("CREATE TEXT INDEX entity_id IF NOT EXISTS FOR (n:OntologyEntity) ON (n.id)");
                indexCommands.add("CREATE TEXT INDEX class_id IF NOT EXISTS FOR (n:OntologyClass) ON (n.id)");
                indexCommands.add("CREATE TEXT INDEX property_id IF NOT EXISTS FOR (n:OntologyProperty) ON (n.id)");
                indexCommands.add("CREATE TEXT INDEX individual_id IF NOT EXISTS FOR (n:OntologyIndividual) ON (n.id)");
                indexCommands.add("CREATE TEXT INDEX ontology_ont_id IF NOT EXISTS FOR (n:Ontology) ON (n.ontologyId)");
                indexCommands.add("CREATE TEXT INDEX entity_ont_id IF NOT EXISTS FOR (n:OntologyEntity) ON (n.ontologyId)");
                indexCommands.add("CREATE TEXT INDEX class_ont_id IF NOT EXISTS FOR (n:OntologyClass) ON (n.ontologyId)");
                indexCommands.add("CREATE TEXT INDEX property_ont_id IF NOT EXISTS FOR (n:OntologyProperty) ON (n.ontologyId)");
                indexCommands.add("CREATE TEXT INDEX individual_ont_id IF NOT EXISTS FOR (n:OntologyIndividual) ON (n.ontologyId)");
                for (String command : indexCommands)
                    try{
                        session.run(command);
                    } catch(Exception e){
                        System.out.println("Could not create constraint: "+e.getMessage());
                    }

                System.out.println("Start Neo4J Modification...");
                if(cmd.hasOption("i")){
                    File dir = new File(directory);
                    List<File> files = listFiles(dir.listFiles());
                    executeBatchedNodeQueries(files, session,batchSize);
                    executeBatchedRelationshipQueries(files,session,batchSize);
                } else
                    for(String ontology : ontologiesToBeRemoved.split(","))
                        try {
                            session.run(generateOntologyDeleteQuery(ontology));
                        } catch (Exception e){
                            e.printStackTrace();
                        }
            }
        }
    }
}
