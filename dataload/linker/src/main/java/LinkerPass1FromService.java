import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LinkerPass1FromService extends ServiceBase{

    public static class LinkerPass1Result {

	// entity IRI -> all definitions of that IRI from ontologies
        Map<String, EntityDefinitionSet> iriToDefinitions = new HashMap<>();

	// ontology IRI -> IDs for that ontology (usually only 1)
	Map<String, Set<String>> ontologyIriToOntologyIds = new HashMap<>();

	// preferred prefix -> ontology IDs with that prefix (usually only 1)
	Map<String, Set<String>> preferredPrefixToOntologyIds = new HashMap<>();

	// ontology id -> defined base URIs for that ontology
	Map<String, Set<String>> ontologyIdToBaseUris = new HashMap<>();

	// ontology id -> IDs of ontologies that import at least 1 term from the ontology
	Multimap<String, String> ontologyIdToImportingOntologyIds = LinkedHashMultimap.create();

	// ontology id -> IDs of ontologies it imports at least 1 term from
	Multimap<String, String> ontologyIdToImportedOntologyIds = LinkedHashMultimap.create();

    }

	public static void parseEntitiesForService(String backendUrl, int pageSize, String ontologyId, int noofEntities, Set<String> ontologyBaseUris,LinkerPass1Result result) throws IOException {
        for (int i = 0; i<numberOfPages(noofEntities, pageSize); i++){
            JsonArray terms = getEntitiesAsJsonArray(backendUrl+"/api/v2/ontologies/"+ontologyId+"/entities?size="+pageSize+"&page="+i, null,"elements");
            for (JsonElement term : terms){
                String iri = null;
                JsonElement label = null;
                JsonElement curie = null;
                Set<String> definedBy = new HashSet<>();
                Set<String> types = new HashSet<>();
                iri = term.getAsJsonObject().get("iri").getAsString();
                System.out.println("term iri: "+iri);
                System.out.println("jsoncurie: "+term.getAsJsonObject().get("curie"));
                curie = jsonParser.parse("{\"type\":[\"literal\"],\"value\":\""+term.getAsJsonObject().get("curie").getAsString()+"\"}");
                System.out.println("curie: "+curie);
                JsonElement lelement = term.getAsJsonObject().get("label");
                StringBuilder sb = new StringBuilder();
                if (lelement.isJsonArray()){
                    sb.append("[");
                    for (JsonElement element : lelement.getAsJsonArray()){
                        if (element.isJsonObject())
                            sb.append("{\"type\":[\"literal\"],\"value\":\""+element.getAsJsonObject().get("value").getAsString()+"\"},");
                        else
                            sb.append("{\"type\":[\"literal\"],\"value\":\""+element.getAsString()+"\"},");
                    }
                    int last = sb.length() - 1;
                    sb.replace(last, last + 1, "");
                    sb.append("]");
                } else if (lelement.isJsonPrimitive()){
                    sb.append("{\"type\":[\"literal\"],\"value\":\""+lelement.getAsString()+"\"}");
                } else if (lelement.isJsonObject()){
                    sb.append(lelement.getAsJsonObject().toString());
                } else {
                    sb.append(lelement.getAsJsonNull().toString());
                }
                System.out.println("jsonlabel: "+sb);
                label = jsonParser.parse(sb.toString());
                System.out.println("label: "+label);
                for (JsonElement type : term.getAsJsonObject().get("type").getAsJsonArray()){
                    types.add(type.getAsString());
                    System.out.println("type: "+type.getAsString());
                }

                JsonElement jsonDefinedBy;
                jsonDefinedBy = term.getAsJsonObject().get("http://www.w3.org/2000/01/rdf-schema#isDefinedBy");
                System.out.println("jsonDefinedBy: "+jsonDefinedBy);
                for (Map.Entry entry : term.getAsJsonObject().entrySet()){
                    if(entry.getKey().equals("http://www.w3.org/2000/01/rdf-schema#isDefinedBy"))
                        System.out.println("defBy: "+entry.getValue());
                }
                if(jsonDefinedBy != null && jsonDefinedBy.isJsonArray()) {
                    JsonArray arr = jsonDefinedBy.getAsJsonArray();
                    for(JsonElement isDefinedBy : arr) {
                        if (isDefinedBy.isJsonObject()) {
                            JsonObject obj = isDefinedBy.getAsJsonObject();
                            var value = obj.get("value");
                            if (value.isJsonObject()) {
                                definedBy.add(value.getAsJsonObject().get("value").getAsString());
                            } else
                                definedBy.add(value.getAsString());
                        } else
                            definedBy.add( isDefinedBy.getAsString() );
                    }
                } else if (jsonDefinedBy != null && jsonDefinedBy.isJsonObject()) {
                    JsonObject obj = jsonDefinedBy.getAsJsonObject();
                    var value = obj.get("value");
                    if (value.isJsonObject()) {
                        definedBy.add(value.getAsJsonObject().get("value").getAsString());
                    } else
                        definedBy.add(value.getAsString());
                }
                else if (jsonDefinedBy != null){
                    definedBy.add(jsonDefinedBy.getAsString());
                } else {
                    definedBy.add("");
                }

                if(iri == null) {
                    throw new RuntimeException("entity had no IRI");
                }

                if(types == null) {
                    throw new RuntimeException("entity had no types");
                }

                EntityDefinition entityDefinition = new EntityDefinition();
                entityDefinition.ontologyId = ontologyId;
                entityDefinition.entityTypes = types;
                entityDefinition.label = label;
                entityDefinition.curie = curie;

                EntityDefinitionSet definitionSet = result.iriToDefinitions.get(iri);

                if(definitionSet == null) {
                    definitionSet = new EntityDefinitionSet();
                    result.iriToDefinitions.put(iri, definitionSet);
                }

                definitionSet.definitions.add(entityDefinition);
                definitionSet.ontologyIdToDefinitions.put(ontologyId, entityDefinition);
                definitionSet.definingOntologyIris.addAll(definedBy);

                for(String baseUri : ontologyBaseUris) {
                    if(iri.startsWith(baseUri)) {
                        definitionSet.definingOntologyIds.add(ontologyId);
                    }
                }
            }
        }
	}

	public static LinkerPass1Result run(String backendUrl, int pageSize) throws IOException {

		LinkerPass1Result result = new LinkerPass1Result();
		int nOntologies = 0;
		try {
			JsonArray ontologies = getEntitiesAsJsonArray(backendUrl+"/api/ontologies?size=1000","_embedded","ontologies");

			for (JsonElement ontology : ontologies){
				String ontologyId = null;
				String ontologyIri = null;
				Set<String> ontologyBaseUris = new HashSet<>();
				String preferredPrefix = null;
				nOntologies++;
				int numberOfTerms = 0;
				int numberOfProperties = 0;
				int numberOfIndividuals = 0;

				ontologyId = ontology.getAsJsonObject().get("ontologyId").getAsString();
				numberOfTerms = ontology.getAsJsonObject().get("numberOfTerms").getAsInt();
				numberOfProperties = ontology.getAsJsonObject().get("numberOfProperties").getAsInt();
				numberOfIndividuals = ontology.getAsJsonObject().get("numberOfIndividuals").getAsInt();

				ontologyIri = ontology.getAsJsonObject().getAsJsonObject("config").get("fileLocation").getAsString();
				Set<String> ids = result.ontologyIriToOntologyIds.get(ontologyIri);
				if(ids == null) {
					ids = new HashSet<>();
					ids.add(ontologyId);
					result.ontologyIriToOntologyIds.put(ontologyIri, ids);
				} else {
					ids.add(ontologyId);
				}

				for (JsonElement baseUri : ontology.getAsJsonObject().getAsJsonObject("config").get("baseUris").getAsJsonArray()){
					ontologyBaseUris.add(baseUri.getAsString());
				}
                boolean obo = false;
                if (ontologyIri.contains("purl.obolibrary.org"))
                    obo = true;
                if (obo)
                    preferredPrefix = ontologyId;
                else
				    preferredPrefix = ontology.getAsJsonObject().getAsJsonObject("config").get("preferredPrefix").getAsString();

				ontologyBaseUris.add("http://purl.obolibrary.org/obo/" + preferredPrefix + "_");

				Set<String> idsp = result.preferredPrefixToOntologyIds.get(preferredPrefix);
				if(idsp == null) {
					idsp = new HashSet<>();
					idsp.add(ontologyId);
					result.preferredPrefixToOntologyIds.put(preferredPrefix, idsp);
				} else {
					idsp.add(ontologyId);
				}

				parseEntitiesForService(backendUrl, pageSize, ontologyId, numberOfTerms+numberOfProperties+numberOfIndividuals,ontologyBaseUris,result);

				result.ontologyIdToBaseUris.put(ontologyId, ontologyBaseUris);
				System.out.println("Now have " + nOntologies + " ontologies and " + result.iriToDefinitions.size() + " distinct IRIs");
			}
		}

		catch(Exception e) {
			e.printStackTrace();
		}

		finally {
			//httpclient.close();
		}

		System.out.println("--- Linker Pass 1: Finished scan from service. Establishing defining ontologies...");

		for(var entry : result.iriToDefinitions.entrySet()) {

			EntityDefinitionSet definitions = entry.getValue();
			// definingOntologyIris -> definingOntologyIds
			for(String ontologyIri : definitions.definingOntologyIris) {
				if (result.ontologyIriToOntologyIds.containsKey(ontologyIri)) {
					for(String ontologyId : result.ontologyIriToOntologyIds.get(ontologyIri)) {
						definitions.definingOntologyIds.add(ontologyId);
					}
				}
			}

			for(EntityDefinition def : definitions.definitions) {
				if(def.curie != null && entry.getValue().definingOntologyIds.iterator().hasNext()) {
					JsonObject curieObject = def.curie.getAsJsonObject();
					if(curieObject.has("value")) {
						String curieValue = curieObject.get("value").getAsString();
						if(!curieValue.contains(":")) {
							var definingOntologyId = entry.getValue().definingOntologyIds.iterator().next();
							EntityDefinition definingEntity = entry.getValue().ontologyIdToDefinitions.get(definingOntologyId);
							if (definingEntity != null && definingEntity.curie != null) {
								curieValue = definingEntity.curie.getAsJsonObject().get("value").getAsString();
								curieObject.addProperty("value", curieValue);
								result.iriToDefinitions.put(entry.getKey(), definitions);
							}
						}
					}
				}
				if(definitions.definingOntologyIds.contains(def.ontologyId)) {
					def.isDefiningOntology = true;
				}
			}

			for(EntityDefinition defA : definitions.definitions) {
				if(defA.isDefiningOntology) {
					// The definition "defA" is in a defining ontology. If any other
					// ontologies use this entity and AREN'T defining, they are considered
					// as "importing" from this ontology.
					//
					for(EntityDefinition defB : definitions.definitions) {
						if(!defB.isDefiningOntology) {
							result.ontologyIdToImportedOntologyIds.put(defB.ontologyId, defA.ontologyId);
							result.ontologyIdToImportingOntologyIds.put(defA.ontologyId, defB.ontologyId);
						}
					}
				}
			}

			definitions.definingDefinitions = definitions.definitions.stream().filter(def -> def.isDefiningOntology).collect(Collectors.toSet());
		}

		System.out.println("--- Linker Pass 1 complete for service. Found " + nOntologies + " ontologies and " + result.iriToDefinitions.size() + " distinct IRIs");
		System.out.println("---------------END----------------------");
		return result;
	}
}
