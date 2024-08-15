package uk.ac.ebi.spot.csv2neo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
public class QueryGeneration {

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
        props.put("id",values[0]);
        props.put("_json",values[2]);
        if (titles.length == values.length) {
            for (int i = 3; i < values.length; i++){
                String[] title = titles[i].split(":");
                if (title.length > 1 && title[1].equals("string[]")) {
                    props.put(title[0].replaceAll("\"\"","\""),values[i].split("\\|"));
                } else
                    props.put(title[0].replaceAll("\"\"","\""),values[i]);
            }
        } else {
            System.out.println("titles and values are not equal");
            System.out.println("titles: "+titles.length + " - values: " +values.length);
        }
        Map<String,Object> params = new HashMap<>();
        params.put( "props", props );
        return params;
    }

    public static String generateRelationCreationQuery(String[] titles, String[] values){
        StringBuilder sb = new StringBuilder();

        if (titles.length == values.length){
            sb.append("MATCH (n"+idToLabel(values[0])+" {id: "+"\'"+values[0]+"\'"+"}),")
                    .append("(m"+idToLabel(values[2])+" {id: "+"\'"+values[2]+"\'"+"}) ")
                    .append("WHERE n.id STARTS WITH '"+values[0].split("\\+")[0]+"' AND m.id STARTS WITH '"+values[2].split("\\+")[0]+"' ")
                    .append("AND '"+values[0].split("\\+")[0]+"' IN n.ontologyId AND '"+values[2].split("\\+")[0]+"' IN m.ontologyId ")
                    .append("CREATE (n)-[:")
                    .append("`"+values[1].replace("|","`:`")+"`")
                    .append("]->(m)");
        } else {
            System.out.println("titles and values are not equal");
            System.out.println("titles: "+titles.length + " - values: " +values.length);
        }

        return sb.toString();
    }

    public static String generateOntologyDeleteQuery(String ontologyPrefix){
        return "MATCH (n) WHERE n.id STARTS WITH '"+ontologyPrefix+"' DETACH DELETE n";
    }

    public static String countAllRelationshipsOfOntology(String ontologyPrefix) {
        return "MATCH (n)-[r]-(m) WHERE '"+ontologyPrefix+"' IN n.ontologyId and '"+ontologyPrefix+"' IN m.ontologyId return count(distinct r) as relationships";
    }

    public static String countRelationshipsOfOntology(String ontologyPrefix, String label) {
        return "MATCH (n)-[r:`"+label+"`]-(m) WHERE '"+ontologyPrefix+"' IN n.ontologyId and '"+ontologyPrefix+"' IN m.ontologyId return count(distinct r) as relationships";
    }

    public static String countAllNodesOfOntology(String ontologyPrefix){
        return "MATCH (n) WHERE n.id STARTS WITH '"+ontologyPrefix+"' return count(n) as nodes";
    }

    public static String countNodesOfOntology(String ontologyPrefix, String type){
        return "MATCH (n) WHERE n.id STARTS WITH '"+ontologyPrefix+"' AND '"+type+"' IN n.type return count(n) as nodes";
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
