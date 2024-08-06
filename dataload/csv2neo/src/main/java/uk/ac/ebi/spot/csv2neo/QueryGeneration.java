package uk.ac.ebi.spot.csv2neo;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
public class QueryGeneration {

    public static String generateNodeCreationQuery(String[] titles, String[] values){

        StringBuilder sb = new StringBuilder();

        if (titles.length == values.length) {

            sb.append("CREATE (")
                    .append(":")
                    .append("`"+values[1].replace("|","`:`")+"`")
                    .append(" {");
            sb.append("id: ").append("\'"+values[0]+"\'");

            for (int i = 2; i < values.length; i++) {
                String text = values[i].replaceAll("\"\"","\"").replaceAll("\\\\", "\\\\\\\\").replaceAll("\'","\\\\'");
                sb.append(", ")
                        .append("`"+titles[i].split(":")[0].replaceAll("\"\"","\"")+"`")
                        .append(": ")
                        .append(convertToJSONArray("\'"+text+"\'"));
            }

            sb.append("}")
                    .append(")")
                    .append(" ");
        } else {
            System.out.println("titles and values are not equal");
            System.out.println("titles: "+titles.length + " - values: " +values.length);
        }
        return sb.toString();
    }

    public static String generateBlankNodeCreationQuery(String[] titles, String[] values){

        StringBuilder sb = new StringBuilder();

        if (titles.length == values.length) {

            sb.append("CREATE (")
                    .append(":")
                    .append("`"+values[1].replace("|","`:`")+"`")
                    .append(" $props")

                    .append(")")
                    .append(" ");
        } else {
            System.out.println("titles and values are not equal");
            System.out.println("titles: "+titles.length + " - values: " +values.length);
        }
        return sb.toString();
    }

    public static Map<String,Object> generateProps(String[] titles, String[] values){

        Map<String,Object> props = new HashMap<>();
        if (titles.length == values.length) {
            for (int i = 0; i < values.length; i++) {
                if (i == 0)
                    props.put("id",values[i]);
                if(i !=1){
                    props.put(titles[i].split(":")[0].replaceAll("\"\"","\""),convertToJSONArray(values[i]));
                }
            }

        } else {
            System.out.println("titles and values are not equal");
            System.out.println("titles: "+titles.length + " - values: " +values.length);
        }

        Map<String,Object> params = new HashMap<>();
        params.put( "props", props );

        return params;
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
            sb.append("MATCH (n"+idToLabel(values[0])+" {id: "+"\'"+values[0]+"\'"+"}),")
                    .append("(m"+idToLabel(values[2])+" {id: "+"\'"+values[2]+"\'"+"}) ")
                    .append("WHERE n.id STARTS WITH '"+values[0].split("\\+")[0]+"' AND m.id STARTS WITH '"+values[2].split("\\+")[0]+"' ")
                    .append("AND n.ontologyId = '"+values[0].split("\\+")[0]+"' AND m.ontologyId = '"+values[2].split("\\+")[0]+"' ")
                    .append("CREATE (n)-[:")
                    .append("`"+values[1].replace("|","`:`")+"`")
                    .append("]->(m)");
        } else {
            System.out.println("titles and values are not equal");
            System.out.println("titles: "+titles.length + " - values: " +values.length);
        }

        return sb.toString();
    }

    public static String generateRelationCreationQuery2(String[] titles, String[] values){
        StringBuilder sb = new StringBuilder();
        if (titles.length == values.length){
            sb.append("MATCH (n {id: "+"\'"+values[0]+"\'"+"})-[:")
                    .append("`"+values[1].replace("|","`:`")+"`")
                    .append("]->(m {id: "+"\'"+values[2]+"\'"+"})");
        }

        return sb.toString();
    }

    public static String generateOntologyDeleteQuery(String ontologyPrefix){
        return "MATCH (n) where n.id STARTS WITH '"+ontologyPrefix+"' DETACH DELETE n";
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

    public static String idToLabel(String id){
        String label = switch (id.split("\\+")[1]) {
            case "class" -> ":OntologyClass";
            case "entity" -> ":OntologyEntity";
            case "ontology" -> ":Ontology";
            case "property" -> ":OntologyProperty";
            case "individual" -> ":OntologyIndividual";
            default -> "";
        };
        return label;
    }
}
