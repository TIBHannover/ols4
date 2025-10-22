import com.google.gson.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EntityDefinition {

    String ontologyId;
    Set<String> entityTypes;
    boolean isDefiningOntology;
    JsonElement label;
    JsonElement curie;

    private static String canonicalJson(JsonElement elem) {
        //if (elem == null) return "null";
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(elem); // ensures consistent serialization
    }

    private static JsonArray removeDuplicates(JsonArray array) {
        JsonArray result = new JsonArray();
        Set<String> seen = new HashSet<>();
        Set<String> seenValues = new HashSet<>();
        Gson gson = new Gson();

        for (JsonElement elem : array) {
            // Canonical string representation (ensures order-insensitive comparison)
            String canonical = gson.toJson(elem);
            if (seen.add(canonical)) {
                result.add(elem);
            }
        }

        return result;
    }

    private static boolean intersects(JsonElement elem1,JsonElement elem2) {
        JsonArray result = new JsonArray();
        if  (elem1.isJsonArray() && elem2.isJsonArray()) {
            for (JsonElement e1 : elem1.getAsJsonArray()) {
                for (JsonElement e2 : elem2.getAsJsonArray()) {
                    if (e1.equals(e2)) {
                        result.add(elem1);
                    }
                }
            }
            return result.size() > 0;
        } else {
            return elem1.equals(elem2);
        }
    }

    private static JsonElement reduceJson(JsonElement elem) {
        if(elem.isJsonObject()) {
            Set<String> keys = new HashSet<String>();
            elem.getAsJsonObject().entrySet().forEach(entry -> {if (!entry.getKey().equals("type") && !entry.getKey().equals("value")) {keys.add(entry.getKey());}});
            keys.forEach(key -> elem.getAsJsonObject().remove(key));
        } else if(elem.isJsonArray()) {
            for (JsonElement e : elem.getAsJsonArray()){
                if(e.isJsonObject()){
                    Set<String> keys = new HashSet<String>();
                    e.getAsJsonObject().entrySet().forEach(entry -> {if (!entry.getKey().equals("type") && !entry.getKey().equals("value")) {keys.add(entry.getKey());}});
                    keys.forEach(key -> e.getAsJsonObject().remove(key));
                }
            }
        }
        return elem;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EntityDefinition &&
                ((EntityDefinition) other).ontologyId.equals(ontologyId) &&
                ((EntityDefinition) other).entityTypes.equals(entityTypes) &&
    //            ((EntityDefinition) other).isDefiningOntology == isDefiningOntology &&
                intersects(reduceJson(((EntityDefinition) other).label),reduceJson(label)) &&
                canonicalJson(((EntityDefinition) other).curie).equals(canonicalJson(curie));
    }

    @Override
    public int hashCode() {
        return Objects.hash(ontologyId, entityTypes, canonicalJson(curie));
    }

    @Override
    public String toString() {
        return "EntityDefinition{" +
                "ontologyId='" + ontologyId + '\'' +
                ", entityTypes=" + entityTypes +
                ", isDefiningOntology=" + isDefiningOntology +
                ", label=" + label +
                ", curie=" + curie +
                '}';
    }
}
