package uk.ac.ebi.spot.csv2neo;

import org.neo4j.driver.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
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

    public static List<File> showFiles(File[] files) throws IOException {
        List<File> fileList = new ArrayList<File>();
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Directory: " + file.getAbsolutePath());
                fileList.addAll(showFiles(file.listFiles()));
            } else {
                System.out.println("File: " + file.getAbsolutePath());
                fileList.add(file);
            }
        }

        return fileList;
    }

    public static void generateNEO(List<File> files, Session session) throws IOException {
        for (File file : files){
            if((file.getName().contains("_edges")) || !file.getName().endsWith(".csv"))
                continue;
            fr = new FileReader(file.getAbsolutePath());
            br = new BufferedReader(fr);
            String line = br.readLine();
            String[] titles = {};
            if (line != null)
                titles = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String[] pieces = null;
            while((line = br.readLine())!=null){
                System.out.println(line);
                pieces = split(line,",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                String query = generateNodeCreationQuery(titles,pieces);
                System.out.println("query: "+query);
                try (Transaction tx = session.beginTransaction()) {
                    tx.run(query);
                    tx.commit();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }

        for (File file : files){
            if((!file.getName().contains("_edges")) || !file.getName().endsWith(".csv"))
                continue;
            fr = new FileReader(file.getAbsolutePath());
            br = new BufferedReader(fr);
            String line = br.readLine();
            String[] titles = {};
            if (line != null)
                titles = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String[] pieces = null;
            while((line = br.readLine())!=null){
                System.out.println(line);
                pieces = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                String query = generateRelationCreationQuery(titles,pieces);
                System.out.println("query: "+query);
                try (Transaction tx = session.beginTransaction()) {
                    tx.run(query);
                    tx.commit();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static String[] split(String input, String regex){
        String[] tokens = {};
        char c = '{';
        char d = '\"';
        char e = '}';
        String left = String.valueOf(d) + c;
        String right = String.valueOf(e) + d;
        int countLeftCurly = countOccurrences(input, left);
        int countRightCurly = countOccurrences(input, right);

        if(countLeftCurly == 0 && countRightCurly == 0){
            tokens = input.split(regex);
        } else if(countLeftCurly == countRightCurly && countLeftCurly == 1){
            String[] content = input.split("\"\\{");
            String before = "";
            String after = "";
            String json = "";
            before = content[0];
            if (before.endsWith(","))
                before = before.substring(0,before.length()-1);
            String[] content2 = content[1].split("\\}\"");
            json = String.valueOf(d)+String.valueOf(c)+content2[0]+String.valueOf(e)+String.valueOf(d);
            after = content2[1];
            if(after.startsWith(","))
                after = after.substring(1,after.length());
            String[] beforeArray = before.split(regex);
            String[] afterArray = after.split(regex);
            int length = beforeArray.length + 1 + afterArray.length;
            tokens = new String[length];
            for (int i =0;i<length;i++){
                if(i<beforeArray.length)
                    tokens[i] = beforeArray[i];
                else if(i==beforeArray.length)
                    tokens[i] = json;
                else
                    tokens[i] = afterArray[i-(beforeArray.length+1)];
            }
        }

        return tokens;
    }

    public static int countOccurrences(String input, String pattern) {
        int count = 0;
        int index = 0;

        while ((index = input.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }

        return count;
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

        File dir = new File(directory);
        List<File> files = showFiles(dir.listFiles());

        try (var driver = cmd.hasOption("a") ? GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword)) : GraphDatabase.driver(dbUri)) {
            driver.verifyConnectivity();
            try (var session = driver.session(SessionConfig.builder().withDatabase(db).build())) {
                try{
                    session.run("CREATE CONSTRAINT FOR (n:Ontology) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT FOR (n:OntologyEntity) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT FOR (n:OntologyClass) REQUIRE n.id IS UNIQUE");
                } catch(Exception e){
                    e.printStackTrace();
                }
                System.out.println("kamil");
                if(cmd.hasOption("i"))
                    generateNEO(files,session);
                else
                    for(String ontology : ontologiesToBeRemoved.split(","))
                        session.run(generateOntologyDeleteQuery(ontology));
            }
        }
    }
}
