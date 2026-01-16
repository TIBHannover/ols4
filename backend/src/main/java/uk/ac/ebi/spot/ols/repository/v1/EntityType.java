package uk.ac.ebi.spot.ols.repository.v1;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */

public enum EntityType {
    ONTOLOGY("Ontology"),
    TERM("OntologyClass"),
    PROPERTY("OntologyProperty"),
    INDIVIDUAL("OntologyIndividual");

    private final String propertyName;

    EntityType(String propertyName) {
        this.propertyName = propertyName;
    }

    public static String[] getNames() {
        String[] commands = new String[EntityType.values().length];
        for (int i = 0;i<EntityType.values().length;i++) {
            commands[i] = EntityType.values()[i].getPropertyName();
        }
        return commands;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
