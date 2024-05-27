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

    public static String generateNodeCreationQuery(String[] titles, String[] values){

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
        } else {
            System.out.println("titles and values are not equal");
            System.out.println("titles: "+titles.length + " - values: " +values.length);
            for (String title : titles)
                System.out.println("title: "+title);
        }
        return sb.toString();
    }

    public static String generateNodeSetQuery(String[] titles, String[] values){

        StringBuilder sb = new StringBuilder();

        if (titles.length == values.length){
            sb.append("MATCH (n) where n.id = ").append("\'"+values[0].substring(1, values[0].length() - 1)+"\'").append(" SET ");

            boolean first = true;

            for (int i = 2; i < values.length; i++){
                if(!first)
                    sb.append(" AND ");
                first = false;
                String text = values[i].substring(1, values[i].length() - 1).replaceAll("\"\"","\"").replaceAll("\\\\", "\\\\\\\\").replaceAll("\'","\\\\'");
                sb.append("n.").append("`"+titles[i].substring(1, titles[i].length() - 1).split(":")[0].replaceAll("\"\"","\"")+"`")
                        .append(" = ").append(convertToJSONArray("\'"+text+"\'"));
            }

        }

        return sb.toString();
    }

    public static String generateRelationCreationQuery(String[] titles, String[] values){
        StringBuilder sb = new StringBuilder();

        if (titles.length == values.length){
            sb.append("MATCH (n {id: "+"\'"+values[0].substring(1, values[0].length() - 1)+"\'"+"}),")
                    .append("(m {id: "+"\'"+values[2].substring(1, values[2].length() - 1)+"\'"+"}) ")
                    .append("CREATE (n)-[:")
                    .append("`"+values[1].substring(1, values[1].length() - 1).replace("|","`:`")+"`")
                    .append("]->(m)");
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

    public static String[] split(String input){
        String[] tokens = {};
        char c = '{';
        char d = '\"';
        char e = '}';
        String left = String.valueOf(d) + c;
        String right = String.valueOf(e) + d;
        int countLeftCurly = countOccurrences(input, left);
        int countRightCurly = countOccurrences(input, right);

        if(countLeftCurly == 0 && countRightCurly == 0){
            tokens = input.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
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
            String[] beforeArray = before.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String[] afterArray = after.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
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

    public static void main(String... args) throws IOException {

        // URI examples: "neo4j://localhost", "neo4j+s://xxx.databases.neo4j.io"
        final String dbUri = "neo4j://localhost";
        final String dbUser = "neo4j";
        final String dbPassword = "testtest";

        File dir = new File("/home/giray/Downloads/neo4j-community-5.19.0/asd");
        List<File> files = showFiles(dir.listFiles());

        try (var driver = GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword))) {
            driver.verifyConnectivity();

            // import org.neo4j.driver.SessionConfig

            try (var session = driver.session(SessionConfig.builder().withDatabase("neo4j").build())) {
                // session usage
                try{
                    session.run("CREATE CONSTRAINT FOR (n:Ontology) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT FOR (n:OntologyEntity) REQUIRE n.id IS UNIQUE");
                    session.run("CREATE CONSTRAINT FOR (n:OntologyClass) REQUIRE n.id IS UNIQUE");
                } catch(Exception e){
                    e.printStackTrace();
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
                        System.out.println("2");

                        System.out.println("file: "+file.getName());

                        String query = generateRelationCreationQuery(titles,pieces);
                        //String query2 = generateSetQuery(titles,pieces);
                        System.out.println("query: "+query);
                        //System.out.println("query2: "+query2);

                        try (Transaction tx = session.beginTransaction()) {
                            tx.run(query);
                            tx.commit();
                            tx.close();
                            // use tx.run() to run queries
                            //     tx.commit() to commit the transaction
                            //     tx.rollback() to rollback the transaction
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                        System.gc();
                    }
                }
            }
        }
        System.out.println("kamil");
    }
}
