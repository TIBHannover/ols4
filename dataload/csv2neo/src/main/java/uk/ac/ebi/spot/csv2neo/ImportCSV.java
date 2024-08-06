package uk.ac.ebi.spot.csv2neo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.neo4j.driver.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    public static void generateCreationQueries(List<File> files, Session session, boolean safe) throws IOException {

        for (File file : files) {
            if (!(file.getName().contains("_ontologies") || file.getName().contains("_properties")
                    || file.getName().contains("_individuals") || file.getName().contains("_classes")) || !file.getName().endsWith(".csv"))
                continue;

            Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
            org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
            String[] headers = csvParser.getHeaderNames().toArray(String[]::new);
            for (CSVRecord csvRecord : csvParser) {
                String[] row = csvRecord.toList().toArray(String[]::new);
                String query = generateBlankNodeCreationQuery(headers,row);
                Map<String,Object> params = generateProps(headers,row);
                if(query.isEmpty())
                    System.out.println("empty query for appended line: "+Arrays.toString(row)+" in file: "+file);
                executeQuery(session, params, safe, query);
            }
        }

        for (File file : files){
            if((!file.getName().contains("_edges")) || !file.getName().endsWith(".csv"))
                continue;

            Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
            org.apache.commons.csv.CSVParser csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.POSTGRESQL_CSV.withFirstRecordAsHeader().withTrim());
            String[] headers = csvParser.getHeaderNames().toArray(String[]::new);

            for (CSVRecord csvRecord : csvParser) {
                String[] row = csvRecord.toList().toArray(String[]::new);
                String query = generateRelationCreationQuery(headers,row);
                if(query.isEmpty())
                    System.out.println("empty query for appended line: "+Arrays.toString(row)+" in file: "+file);
                executeQuery(session, null, safe, query);
            }
        }
    }

    private static void executeQuery(Session session, Map<String,Object> params, boolean safe, String query){
        if(safe){
            try (Transaction tx = session.beginTransaction()) {
                if (params != null)
                    tx.run(query, params);
                else
                    tx.run(query);
                tx.commit();
            } catch(Exception e){
                e.printStackTrace();
            }
        } else
            try{
                if (params != null)
                    session.run(query, params);
                else
                    session.run(query);
            } catch (Exception e){
                e.printStackTrace();
            }
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
        options.addOption("s", "safe",false, "execute each neo4j query in transactions or the session");
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

        try (var driver = cmd.hasOption("a") ? GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword)) : GraphDatabase.driver(dbUri)) {
            driver.verifyConnectivity();
            try (var session = driver.session(SessionConfig.builder().withDatabase(db).build())) {
                try{
                    session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (n:Ontology) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (n:OntologyEntity) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (n:OntologyClass) REQUIRE n.id IS UNIQUE");
                } catch(Exception e){
                    e.printStackTrace();
                }
                System.out.println("Start Neo4J Modification...");
                if(cmd.hasOption("i")){
                    File dir = new File(directory);
                    List<File> files = listFiles(dir.listFiles());
                    if(cmd.hasOption("s"))
                        generateCreationQueries(files,session,true);
                    else
                        generateCreationQueries(files,session,false);
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
