import java.util.*;

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

    @Override
    public int hashCode() {
        return Objects.hash(definitions, definingDefinitions, definingOntologyIris, definingOntologyIds, ontologyIdToDefinitions);
    }

    @Override
    public String toString() {
        return "EntityDefinitionSet{" +
                "definitions=" + definitions +
                ", definingDefinitions=" + definingDefinitions +
                ", definingOntologyIris=" + definingOntologyIris +
                ", definingOntologyIds=" + definingOntologyIds +
                ", ontologyIdToDefinitions=" + ontologyIdToDefinitions +
                '}';
    }
}
