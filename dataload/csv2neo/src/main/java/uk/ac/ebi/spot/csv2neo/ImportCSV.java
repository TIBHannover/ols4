package uk.ac.ebi.spot.csv2neo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.neo4j.driver.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    public static void executeBatchedNodeQueries(List<File> files, Driver driver, int batchSize, int poolSize, int attempts) throws IOException, InterruptedException {
        for (File file : files) {
            if (!(file.getName().contains("_ontologies") || file.getName().contains("_properties")
                    || file.getName().contains("_individuals") || file.getName().contains("_classes")) || !file.getName().endsWith(".csv"))
                continue;
            Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
            org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
            String[] headers = csvParser.getHeaderNames().toArray(String[]::new);
            List<List<CSVRecord>> splitRecords = splitList(csvParser.getRecords(),batchSize);
            CountDownLatch latch = new CountDownLatch(splitRecords.size());
            ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
            for (List<CSVRecord> records : splitRecords){
                NodeCreationQueryTask task = new NodeCreationQueryTask(driver,latch, records,headers,file,attempts);
                executorService.submit(task);
            }
            latch.await();
            executorService.shutdown();
        }
    }

    public static void executeBatchedRelationshipQueries(List<File> files, Driver driver, int batchSize, int poolSize, int attempts) throws IOException, InterruptedException {
        for (File file : files) {
            if ((!file.getName().contains("_edges")) || !file.getName().endsWith(".csv"))
                continue;

            Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
            org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
            String[] headers = csvParser.getHeaderNames().toArray(String[]::new);
            List<List<CSVRecord>> splitRecords = splitList(csvParser.getRecords(), batchSize);
            CountDownLatch latch = new CountDownLatch(splitRecords.size());
            ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
            for (List<CSVRecord> records : splitRecords){
                RelationShipCreationQueryTask task = new RelationShipCreationQueryTask(driver,latch,records,headers,file, attempts);
                executorService.submit(task);
            }
            latch.await();
            executorService.shutdown();
        }
    }

    /*
     * Files should be the _ontologies.csv files
     * */
    public static Map<String,Integer> displayIngested(List<File> files, Driver driver) throws IOException {
        System.out.println("---Ingestion Summary---");
        Map<String,Integer> countRecords = new HashMap<String,Integer>();
        for (File file : files){
            Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
            org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
            List<CSVRecord> records = csvParser.getRecords();
            for (CSVRecord record : records){
                try (Session session = driver.session()){
                    String ontology = record.get(0).split("\\+")[0];
                    var resultN = session.run(countNodesOfOntology(ontology,"ontology"));
                    int nodes = resultN.next().get("nodes").asInt();
                    countRecords.put(ontology+"_ontologies.csv",nodes);
                    System.out.println(nodes+" ontologies are ingested from "+ontology);
                    resultN = session.run(countNodesOfOntology(ontology,"property"));
                    nodes = resultN.next().get("nodes").asInt();
                    countRecords.put(ontology+"_properties.csv",nodes);
                    System.out.println(nodes+" properties are ingested from "+ontology);
                    resultN = session.run(countNodesOfOntology(ontology,"individual"));
                    nodes = resultN.next().get("nodes").asInt();
                    countRecords.put(ontology+"_individuals.csv",nodes);
                    System.out.println(nodes+" individuals are ingested from "+ontology);
                    resultN = session.run(countNodesOfOntology(ontology,"class"));
                    nodes = resultN.next().get("nodes").asInt();
                    countRecords.put(ontology+"_classes.csv",nodes);
                    System.out.println(nodes+" classes are ingested from "+ontology);
                    var resultR = session.run(countAllRelationshipsOfOntology(ontology));
                    int relationships = resultR.next().get("relationships").asInt();
                    countRecords.put(ontology+"_relationships.csv",relationships);
                    System.out.println(relationships+" relationships are ingested from "+ontology);
                }
            }

        }
        return countRecords;
    }

    public static Map<String,Integer> displayCSV(List<File> files) throws IOException {
        Map<String,Integer> records = new HashMap<String, Integer>();
        System.out.println("---Ingestion Plan---");
        long noofRelationships = 0;
        long noofNodes = 0;
        for (File file : files){
            if (file.getName().endsWith("_edges.csv")){
                try {
                    Path path = Paths.get(file.getAbsolutePath());
                    int noofRecords = (int) Files.lines(path).count() - 1;
                    records.put(file.getName(),noofRecords);
                    noofRelationships += noofRecords;
                    System.out.println(noofRecords+" records has been identified in "+file.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (file.getName().endsWith("_ontologies.csv") || file.getName().endsWith("_properties.csv") || file.getName().endsWith("_classes.csv") || file.getName().endsWith("_individuals.csv")){
                Path path = Paths.get(file.getAbsolutePath());
                Reader reader = Files.newBufferedReader(path);
                org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
                int noofRecords = csvParser.getRecords().size();
                int noofNewLines = (int) Files.lines(path).count() -1;
                records.put(file.getName(),noofRecords);
                noofNodes += noofRecords;
                if (noofRecords != noofNewLines)
                    System.out.println("Warning: "+noofRecords+" records has been identified in contrast to "+noofNewLines+" new lines in "+file.getName());
                else
                    System.out.println(noofRecords+" records has been identified in "+file.getName());
            }
        }
        System.out.println("Total number of nodes that will be ingested in csv: " + noofNodes);
        System.out.println("Total Number of relationships that will be ingested in csv: " + noofRelationships);
        return records;
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
        options.addOption("m", "mode",true, "ingest(i), remove(rm) or display(d) ontologies");
        options.addOption("a", "authenticated",false, "use authentication");
        options.addOption("u", "user",true, "neo4j user name");
        options.addOption("pw", "password",true, "neo4j user password");
        options.addOption("uri", "database_uri",true, "neo4j database uri");
        options.addOption("db", "database",true, "neo4j database name");
        options.addOption("o", "ontologies",true, "ontologies to be removed or displayed by commas");
        options.addOption("d", "directory",true, "neo4j csv import directory");
        options.addOption("bs", "batch_size",true, "batch size for splitting queries into multiple transactions.");
        options.addOption("ps", "pool_size",true, "number of threads in the pool");
        options.addOption("t", "attempts",true, "number of attempts for a particular batch");
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
        final String ontologyPrefixes = cmd.hasOption("o") ? cmd.getOptionValue("o") : "";
        final int batchSize = cmd.hasOption("bs") && Integer.parseInt(cmd.getOptionValue("bs"))>0 ? Integer.parseInt(cmd.getOptionValue("bs")) : 1000;
        final int poolSize = cmd.hasOption("ps") && Integer.parseInt(cmd.getOptionValue("ps"))>0 ? Integer.parseInt(cmd.getOptionValue("ps")) : 20;
        final int attempts = cmd.hasOption("t") ? Integer.parseInt(cmd.getOptionValue("t")) : 5;

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
                if(cmd.hasOption("m")){
                    if (cmd.getOptionValue("m").equals("i")){
                        File dir = new File(directory);
                        List<File> files = listFiles(dir.listFiles());
                        Map<String,Integer> planned = displayCSV(files);
                        executeBatchedNodeQueries(files,driver,batchSize,poolSize,attempts);
                        executeBatchedRelationshipQueries(files,driver,batchSize, poolSize,attempts);
                        Map<String,Integer> ingested = displayIngested(files.stream().filter(f -> f.getName().endsWith("_ontologies.csv")).collect(Collectors.toUnmodifiableList()), driver);

                        Set<String> keysP = planned.keySet();
                        Set<String> keysI = ingested.keySet();
                        keysP.addAll(keysI);
                        for (String key : keysP){
                            System.out.println("Planned: "+planned.getOrDefault(key,Integer.valueOf(-1))+" and Ingested: "+ingested.getOrDefault(key,Integer.valueOf(-1)));
                        }
                    } else if (cmd.getOptionValue("m").equals("rm")){
                        for(String ontology : ontologyPrefixes.split(",")){
                            try {
                                session.run(generateOntologyDeleteQuery(ontology));
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    } else if (cmd.getOptionValue("m").equals("d")){
                        for(String ontology : ontologyPrefixes.split(",")){
                            var resultN = session.run(countAllNodesOfOntology(ontology));
                            System.out.println("Number of nodes in ontology "+ontology+" is "+resultN.next().get("nodes").asInt());
                            var resultR = session.run(countAllRelationshipsOfOntology(ontology));
                            System.out.println("Number of relationships in ontology "+ontology+" is "+resultR.next().get("relationships").asInt());
                        }
                    }
                } else {
                    System.out.println("Mode should be i, d, or rm");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
