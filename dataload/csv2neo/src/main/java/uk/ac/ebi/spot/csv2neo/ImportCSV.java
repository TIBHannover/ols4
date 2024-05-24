package uk.ac.ebi.spot.csv2neo;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String generateOntologyCreationQuery(String[] titles, String[] values){

        StringBuilder sb = new StringBuilder();

        if (titles.length == values.length) {

            sb.append("CREATE (")
                    .append(":")
                    .append("`"+values[1].substring(1, values[1].length() - 1).replace("|","`:`")+"`")
                    .append(" {");
            sb.append("id: ").append("\'"+values[0].substring(1, values[0].length() - 1)+"\'");

            for (int i = 2; i < values.length; i++) {
                String text = values[i].substring(1, values[i].length() - 1).replaceAll("\"\"","\"").replaceAll("\\\\", "\\\\\\\\").replaceAll("\'","\\\\'");
                sb.append(", ")
                        .append("`"+titles[i].substring(1, titles[i].length() - 1).split(":")[0].replaceAll("\"\"","\"")+"`")
                        .append(": ")
                        .append(convertToJSONArray("\'"+text+"\'"));
            }

            sb.append("}")
                    .append(")")
                    .append(" ");
        }
        return sb.toString();
    }

    public static String generateClassCreationQuery(String[] titles, String[] values){

        StringBuilder sb = new StringBuilder();


        if (titles.length == values.length) {

            sb.append("CREATE (")
                    //         .append(values[0].substring(1, values[0].length() - 1))
                    .append(":")
                    .append(values[1].substring(1, values[1].length() - 1).replace('|',':'))
                    .append(" {");
            sb.append("id: ").append("\'"+values[0].substring(1, values[0].length() - 1)+"\'");

            for (int i = 2; i < values.length; i++) {
                sb.append(", ");

                if (titles[i].substring(1, titles[i].length() - 1).split(":")[0].replaceAll("\"\"","\"").replace("\\","__").length() > 30)
                    sb.append("`"+titles[i].substring(1, titles[i].length() - 1).split(":")[0].replaceAll("\"\"","\"").replace("\\","__")+"`");
                else
                    sb.append(titles[i].substring(1, titles[i].length() - 1).split(":")[0].replaceAll("\"\"","\"").replace("\\","__"));
                sb.append(": ").append(convertToJSONArray("\'"+values[i].substring(1, values[i].length() - 1).replaceAll("\"\"","\"").replace("\\","__")+"\'"));

            }

            sb.append("}")
                    .append(")")
                    .append(" ");
        }
        return sb.toString();
    }

    public static String generateClassSetQuery(String[] titles, String[] values){

        StringBuilder sb = new StringBuilder();

        if (titles.length == values.length){
            sb.append("MATCH (n) where n.id = ").append("\'"+values[0].substring(1, values[0].length() - 1)+"\'").append(" SET ");

            boolean first = true;

            for (int i = 2; i < values.length; i++){

                if (titles[i].substring(1, titles[i].length() - 1).split(":")[0].replaceAll("\"\"","\"").replace("\\","__").length() <= 30)
                    continue;

                if(!first)
                    sb.append(" AND ");
                first = false;

                sb.append("n.").append(titles[i].substring(1, titles[i].length() - 1).split(":")[0].replaceAll("\"\"","\"").replace("\\","__"))
                        .append(" = ").append(convertToJSONArray("\'"+values[i].substring(1, values[i].length() - 1).replaceAll("\"\"","\"").replace("\\","__")+"\'"));

            }

        }

        return sb.toString();
    }

    public static String convertToJSONArray(String input){
        if (input.contains("|")){
            input = input.substring(1,input.length()-1);
            String[] sarray = input.split("\\|");
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0;i<sarray.length;i++){
                sb.append("\'").append(sarray[i]).append("\'");
                if(i< sarray.length -1)
                    sb.append(",");

            }
            sb.append("]");
            input = sb.toString();
        }

        return input;
    }

    public static String decode(String input) {
        Pattern pattern = Pattern.compile("\\\\u[0-9a-fA-F]{4}");
        Matcher matcher = pattern.matcher(input);

        StringBuilder decodedString = new StringBuilder();

        while (matcher.find()) {
            String unicodeSequence = matcher.group();
            char unicodeChar = (char) Integer.parseInt(unicodeSequence.substring(2), 16);
            matcher.appendReplacement(decodedString, Character.toString(unicodeChar));
        }

        matcher.appendTail(decodedString);
        return decodedString.toString();
    }

    public static void main(String... args) throws IOException {

        // URI examples: "neo4j://localhost", "neo4j+s://xxx.databases.neo4j.io"
        final String dbUri = "neo4j://localhost";
        final String dbUser = "neo4j";
        final String dbPassword = "test";

        File dir = new File("/home/giray/Downloads/neo4j-community-5.19.0/out");
        List<File> files = showFiles(dir.listFiles());

        try (var driver = GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword))) {
            driver.verifyConnectivity();

            // import org.neo4j.driver.SessionConfig

            try (var session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
                // session usage

                for (File file : files){
                    if(file.getName().contains("_edges") || !file.getName().endsWith(".csv"))
                        continue;
                    // classes doesnt work ontologies work. {_json: '{
                    fr = new FileReader(file.getAbsolutePath());
                    br = new BufferedReader(fr);
                    String line = br.readLine();
                    String[] titles = {};
                    if (line != null)
                        titles = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    while((line = br.readLine())!=null){
                        String[] pieces = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                        System.out.println("file: "+file.getName());

                        String query = generateOntologyCreationQuery(titles,pieces);
                        String query2 = generateClassSetQuery(titles,pieces);
                        System.out.println("query: "+query);
                        //System.out.println("query2: "+query2);

                        try (Transaction tx = session.beginTransaction()) {
                            // "CREATE (o:Organization {id: randomuuid(), createdDate: datetime()})"
                            tx.run(query);
                            //tx.run(query2);
                            tx.commit();
                            // use tx.run() to run queries
                            //     tx.commit() to commit the transaction
                            //     tx.rollback() to rollback the transaction
                        } /*catch(Exception e){
                            e.printStackTrace();
                        }*/
                    }
                }
            }
        }
    }
}
