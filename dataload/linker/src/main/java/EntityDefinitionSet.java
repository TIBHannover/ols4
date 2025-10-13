import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntityDefinitionSet {
    Set<EntityDefinition> definitions = new HashSet<>();
    Set<EntityDefinition> definingDefinitions = new HashSet<>();
    Set<String> definingOntologyIris = new HashSet<>();
    Set<String> definingOntologyIds = new HashSet<>();
    Map<String, EntityDefinition> ontologyIdToDefinitions = new HashMap<>();


    @Override
    public boolean equals(Object other) {
        return other instanceof EntityDefinitionSet
                && ((EntityDefinitionSet) other).definitions.equals(definitions)
                && ((EntityDefinitionSet) other).definingDefinitions.equals(definingDefinitions)
                && ((EntityDefinitionSet) other).definingOntologyIris.equals(definingOntologyIris)
                && ((EntityDefinitionSet) other).definingOntologyIds.equals(definingOntologyIds)
                && ((EntityDefinitionSet) other).ontologyIdToDefinitions.equals(ontologyIdToDefinitions);
    }

}
