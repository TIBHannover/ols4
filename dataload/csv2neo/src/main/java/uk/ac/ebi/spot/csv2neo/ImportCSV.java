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

    public static void generateCreationQueries(List<File> files, Session session, boolean safe) throws IOException, java.text.ParseException {
        for (File file : files){
            if(!(file.getName().contains("_ontologies") || file.getName().contains("_properties")
                    || file.getName().contains("_individuals") || file.getName().contains("_classes")) || !file.getName().endsWith(".csv"))
                continue;
            fr = new FileReader(file.getAbsolutePath());
            br = new BufferedReader(fr);
            String line = br.readLine();
            String[] titles = {};
            if (line != null)
                titles = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String[] pieces = null;
            StringBuilder sb = new StringBuilder();
            boolean started = false;
            while((line = br.readLine())!=null){
                String appendedLine = "";

                if (line.startsWith("\"") && line.endsWith("\"")){
                    if(started){
                        if (line.startsWith("\",\"") && !sb.toString().isEmpty()) {
                            sb.append(line);
                            appendedLine = sb.toString();
                            sb.setLength(0);
                            started = false;
                        }
                        else
                            throw new IOException("file: "+file+" - line: "+line);
                    }
                    else
                        appendedLine = line;
                } else if (line.startsWith("\"") && !line.endsWith("\"")){
                    if(started){
                        if (line.startsWith("\",\"")) {
                            sb.append(line);
                        }
                        else
                            throw new IOException("file: "+file+" - line: "+line);
                    }
                    else {
                        sb.append(line);
                        started = true;
                    }
                } else if (!line.startsWith("\"") && !line.endsWith("\"")){
                    if(!started)
                        throw new IOException("file: "+file+" - line: "+line);
                    else
                        sb.append(line);

                } else if (!line.startsWith("\"") && line.endsWith("\"") && !sb.toString().isEmpty()){
                    if(!started)
                        throw new IOException("file: "+file+" - line: "+line);
                    else {
                        sb.append(line);
                        appendedLine = sb.toString();
                        sb.setLength(0);
                        started = false;
                    }
                }

                if (appendedLine.isEmpty())
                    continue;

                pieces = split(appendedLine, List.of(titles).indexOf("\"_json\""),titles.length,",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                String query = generateNodeCreationQuery(titles,pieces);
                if(query.isEmpty())
                    System.out.println("empty query for appended line: "+appendedLine+" in file: "+file);
                if(safe){
                    try (Transaction tx = session.beginTransaction()) {
                        tx.run(query);
                        tx.commit();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                } else
                    try{
                        session.run(query);
                    } catch (Exception e){
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
            StringBuilder sb = new StringBuilder();
            boolean started = false;
            while((line = br.readLine())!=null){
                String appendedLine = "";
                if (line.startsWith("\"") && line.endsWith("\"")){
                    if(started){
                        if (line.startsWith("\",\"") && !sb.toString().isEmpty()) {
                            sb.append(line);
                            appendedLine = sb.toString();
                            sb.setLength(0);
                            started = false;
                        }
                        else
                            throw new IOException("file: "+file+" - line: "+line);
                    }
                    else
                        appendedLine = line;
                } else if (line.startsWith("\"") && !line.endsWith("\"")){
                    if(started){
                        if (line.startsWith("\",\"")) {
                            sb.append(line);
                        }
                        else
                            throw new IOException("file: "+file+" - line: "+line);
                    }
                    else {
                        sb.append(line);
                        started = true;
                    }
                } else if (!line.startsWith("\"") && !line.endsWith("\"")){
                    if(!started)
                        throw new IOException("file: "+file+" - line: "+line);
                    else
                        sb.append(line);
                } else if (!line.startsWith("\"") && line.endsWith("\"") && !sb.toString().isEmpty()){
                    if(!started)
                        throw new IOException("file: "+file+" - line: "+line);
                    else {
                        sb.append(line);
                        appendedLine = sb.toString();
                        sb.setLength(0);
                        started = false;
                    }
                }

                if (appendedLine.isEmpty())
                    continue;

                pieces = appendedLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                String query = generateRelationCreationQuery(titles,pieces);
                if(query.isEmpty())
                    System.out.println("empty query for appended line: "+appendedLine+" in file: "+file);
                if(safe){
                    try (Transaction tx = session.beginTransaction()) {
                        tx.run(query);
                        tx.commit();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                } else
                    try{
                        session.run(query);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

            }
        }
    }

    public static String[] split(String input, int jsonIndex,int titlesLength, String regex) throws java.text.ParseException {
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
        } else if (countLeftCurly >= 1 && countRightCurly >= countLeftCurly){
            String before = "";
            String after = "";
            String json = "";
            int start = 0;
            int end = 0;

            int countDoubleQuotes = 0;
            int countCommas = 0;
            for (int i = 0; i < input.length(); i++){
                if (input.charAt(i) == '"'){
                    countDoubleQuotes++;
                    if (countDoubleQuotes % 2 == 0)
                        if(input.charAt(i+1) == ',')
                            countCommas++;
                }

                if (countDoubleQuotes >= 2*jsonIndex && countCommas == jsonIndex){
                    before = input.substring(0,i+1);
                    start = i+1;
                    break;
                }

            }

            countDoubleQuotes = 0;
            countCommas = 0;
            for (int j = input.length()-1;j>-1;j--){
                if (input.charAt(j) == '"'){
                    countDoubleQuotes++;
                    if (countDoubleQuotes % 2 == 0)
                        if(input.charAt(j-1) == ',')
                            countCommas++;
                }

                if (countDoubleQuotes >= 2*(titlesLength - jsonIndex -1) && countCommas == titlesLength - jsonIndex -1){
                    after = input.substring(j);
                    end = j;
                    break;
                }
            }

            json = input.substring(start,end);

            String[] beforeArray = before.split(regex);
            String[] afterArray = after.split(regex);
            int length = beforeArray.length + 1 + afterArray.length;

            if (length == titlesLength){
                tokens = new String[length];
                for (int i =0;i<length;i++){
                    if(i<beforeArray.length)
                        tokens[i] = beforeArray[i];
                    else if(i==beforeArray.length)
                        tokens[i] = json;
                    else
                        tokens[i] = afterArray[i-(beforeArray.length+1)];
                }
            } else
                throw new java.text.ParseException("before: "+before+"\n"
                        +"json: "+json+"\n"
                        +"after: "+after+"\n"
                        +"Resulted in "+length+" for "+titlesLength+" titles!!!",countRightCurly);

        } else {
            throw new java.text.ParseException(input+"\n Number of left curly braces: "+countLeftCurly+" - Number of right curly braces: "+countRightCurly+" - !!!",countRightCurly);
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
                    session.run("CREATE CONSTRAINT FOR (n:Ontology) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT FOR (n:OntologyEntity) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT FOR (n:OntologyClass) REQUIRE n.id IS UNIQUE");
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

            } catch (java.text.ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
