import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static uk.ac.ebi.ols.shared.DefinedFields.BASE_URI;

public class LinkerPass1 {

    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();
	private static CloseableHttpClient httpclient = HttpClients.createDefault();
	// Create a custom response handler
	private static ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

		@Override
		public String handleResponse(
				final HttpResponse response) throws ClientProtocolException, IOException {
			int status = response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300) {
				HttpEntity entity = response.getEntity();
				return entity != null ? EntityUtils.toString(entity) : null;
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}
		}

	};

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

    public static LinkerPass1Result run(String inputJsonFilename) throws IOException {

        LinkerPass1Result result = new LinkerPass1Result();

        FileInputStream is = new FileInputStream(inputJsonFilename);
        InputStreamReader reader = new InputStreamReader(is);
        JsonReader jsonReader = new JsonReader(reader);

        int nOntologies = 0;

        System.out.println("--- Linker Pass 1: Scanning " + inputJsonFilename);

        jsonReader.beginObject();

        while (jsonReader.peek() != JsonToken.END_OBJECT) {
            String name = jsonReader.nextName();

            if (name.equals("ontologies")) {
                jsonReader.beginArray();

                while(jsonReader.peek() != JsonToken.END_ARRAY) {

                    jsonReader.beginObject(); // ontology

					String ontologyId = null;
					Set<String> ontologyBaseUris = new HashSet<>();

					String key;

					while(jsonReader.peek() != JsonToken.END_OBJECT) {

					key = jsonReader.nextName();

					if(key.equals("ontologyId")) {

						ontologyId = jsonReader.nextString();
						++nOntologies;
						System.out.println("Scanning ontology: " + ontologyId);

					} else if(key.equals("iri")) {

						if(ontologyId == null)
							throw new RuntimeException("missing ontologyId");

						String ontologyIri = jsonReader.nextString();

						Set<String> ids = result.ontologyIriToOntologyIds.get(ontologyIri);
						if(ids == null) {
							ids = new HashSet<>();
							ids.add(ontologyId);
							result.ontologyIriToOntologyIds.put(ontologyIri, ids);
						} else {
							ids.add(ontologyId);
						}

					} else if(key.equals(BASE_URI.getText())) {

						JsonArray baseUris = jsonParser.parse(jsonReader).getAsJsonArray();
						for(JsonElement baseUri : baseUris) {
							ontologyBaseUris.add(baseUri.getAsString());
						}

					} else if(key.equals("preferredPrefix")) {

						String preferredPrefix = jsonReader.nextString();

						ontologyBaseUris.add("http://purl.obolibrary.org/obo/" + preferredPrefix + "_");

						Set<String> ids = result.preferredPrefixToOntologyIds.get(preferredPrefix);
						if(ids == null) {
							ids = new HashSet<>();
							ids.add(ontologyId);
							result.preferredPrefixToOntologyIds.put(preferredPrefix, ids);
						} else {
							ids.add(ontologyId);
						}

					} else if(key.equals("classes")) {

						if(ontologyId == null)
							throw new RuntimeException("missing ontologyId");

						parseEntityArray(jsonReader, "class", ontologyId, ontologyBaseUris, result);

					} else if(key.equals("properties")) {

						if(ontologyId == null)
							throw new RuntimeException("missing ontologyId");

						parseEntityArray(jsonReader, "property", ontologyId, ontologyBaseUris, result);

					} else if(key.equals("individuals")) {

						if(ontologyId == null)
							throw new RuntimeException("missing ontologyId");

						parseEntityArray(jsonReader, "individual", ontologyId, ontologyBaseUris, result);

					}  else {
						jsonReader.skipValue();
					}
		    	}

                jsonReader.endObject(); // ontology

		    	result.ontologyIdToBaseUris.put(ontologyId, ontologyBaseUris);

				System.out.println("Now have " + nOntologies + " ontologies and " + result.iriToDefinitions.size() + " distinct IRIs");
			}

				jsonReader.endArray();

            } else {

                jsonReader.skipValue();

            }
        }

        jsonReader.endObject();
        jsonReader.close();

        System.out.println("--- Linker Pass 1: Finished scan. Establishing defining ontologies...");

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

        System.out.println("--- Linker Pass 1 complete. Found " + nOntologies + " ontologies and " + result.iriToDefinitions.size() + " distinct IRIs");

        return result;
    }

    public static void parseEntityArray(JsonReader jsonReader, String entityType, String ontologyId, Set<String> ontologyBaseUris, LinkerPass1Result result) throws IOException {
        jsonReader.beginArray();

        while(jsonReader.peek() != JsonToken.END_ARRAY) {
            parseEntity(jsonReader, entityType, ontologyId, ontologyBaseUris, result);
        }

        jsonReader.endArray();
    }

    public static void parseEntity(JsonReader jsonReader, String entityType, String ontologyId,  Set<String> ontologyBaseUris,LinkerPass1Result result) throws IOException {
        jsonReader.beginObject();

        String iri = null;
        JsonElement label = null;
		JsonElement curie = null;
		Set<String> definedBy = new HashSet<>();
		Set<String> types = null;

        while(jsonReader.peek() != JsonToken.END_OBJECT) {
            String key = jsonReader.nextName();

            if(key.equals("iri")) {
                iri = jsonReader.nextString();
            } else if(key.equals("label")) {
                label = jsonParser.parse(jsonReader);
			} else if(key.equals("curie")) {
				curie = jsonParser.parse(jsonReader);
			} else if(key.equals("type")) {
                types = gson.fromJson(jsonReader, Set.class);

			} else if(key.equals("http://www.w3.org/2000/01/rdf-schema#isDefinedBy")) {
				JsonElement jsonDefinedBy = jsonParser.parse(jsonReader);
				if(jsonDefinedBy.isJsonArray()) {
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
				} else if (jsonDefinedBy.isJsonObject()) {
					JsonObject obj = jsonDefinedBy.getAsJsonObject();
					var value = obj.get("value");
					if (value.isJsonObject()) {
						definedBy.add(value.getAsJsonObject().get("value").getAsString());
					} else
						definedBy.add(value.getAsString());
				}
				else {
					definedBy.add(jsonDefinedBy.getAsString());
				}
			} else {
                jsonReader.skipValue();
            }

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

        jsonReader.endObject();
    }

	public static JsonArray getEntitiesAsJsonArray(String uri, String property, String arrayName) throws IOException {
		HttpGet httpget = new HttpGet(uri);
		System.out.println("Executing request " + httpget.getRequestLine());
		String responseBody = httpclient.execute(httpget, responseHandler);
		System.out.println("----------------------------------------");

		JsonElement jElement = jsonParser.parse(responseBody);

		if (property == null)
			return jElement.getAsJsonObject().getAsJsonArray(arrayName); // for v2 calls
		else
			return jElement.getAsJsonObject().getAsJsonObject(property).getAsJsonArray(arrayName); // for v1 calls

	}

	public static void parseEntitiesForService(String backendUrl, String ontologyId,  int noofEntities, Set<String> ontologyBaseUris,LinkerPass1Result result) throws IOException {
		JsonArray terms = getEntitiesAsJsonArray(backendUrl+"/api/v2/ontologies/"+ontologyId+"/entities?size="+noofEntities,null,"elements");

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
			System.out.println("jsonlabel: "+term.getAsJsonObject().get("label"));
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (JsonElement labelElement : term.getAsJsonObject().get("label").getAsJsonArray()){
				sb.append("{\"type\":[\"literal\"],\"value\":\""+labelElement.getAsString()+"\"},");
			}
			int last = sb.length() - 1;
			sb.replace(last, last + 1, "");
			sb.append("]");

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

	public static LinkerPass1Result runForService(String backendUrl) throws IOException {

		LinkerPass1Result result = new LinkerPass1Result();
		int nOntologies = 0;

		try {
			JsonArray ontologies = getEntitiesAsJsonArray(backendUrl+"/api/ontologies/","_embedded","ontologies");

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
				numberOfIndividuals = ontology.getAsJsonObject().get("numberOfTerms").getAsInt();

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

				parseEntitiesForService(backendUrl, ontologyId, numberOfTerms+numberOfProperties+numberOfIndividuals,ontologyBaseUris,result);

				result.ontologyIdToBaseUris.put(ontologyId, ontologyBaseUris);
				System.out.println("Now have " + nOntologies + " ontologies and " + result.iriToDefinitions.size() + " distinct IRIs");
			}
		}

		catch(Exception e) {
			e.printStackTrace();
		}

		finally {
			httpclient.close();
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
