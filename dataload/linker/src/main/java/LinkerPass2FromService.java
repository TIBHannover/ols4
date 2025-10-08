import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static uk.ac.ebi.ols.shared.DefinedFields.*;

public class LinkerPass2FromService {

    private static final JsonParser jsonParser = new JsonParser();

    public static final OboDatabaseUrlService dbUrls = new OboDatabaseUrlService();
    public static final Bioregistry bioregistry = new Bioregistry();

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

    public static void run(String backendUrl, String outputJsonFilename, LevelDB leveldb, LinkerPass1FromService.LinkerPass1Result pass1Result) throws IOException {


        JsonArray ontologies = getEntitiesAsJsonArray(backendUrl+"/api/ontologies/","_embedded","ontologies");
        JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(new FileOutputStream(outputJsonFilename)));
        jsonWriter.setIndent("  ");

        System.out.println("--- Linker Pass 2: Processing " + backendUrl);
        int nOntologies = 0;

        jsonWriter.beginObject();
        jsonWriter.name("ontologies");
        jsonWriter.beginArray();
        for (JsonElement ontology : ontologies){

            int numberOfTerms = 0;
            int numberOfProperties = 0;
            int numberOfIndividuals = 0;

            jsonWriter.beginObject();
            String ontologyId = ontology.getAsJsonObject().get("ontologyId").getAsString();

            numberOfTerms = ontology.getAsJsonObject().get("numberOfTerms").getAsInt();
            numberOfProperties = ontology.getAsJsonObject().get("numberOfProperties").getAsInt();
            numberOfIndividuals = ontology.getAsJsonObject().get("numberOfIndividuals").getAsInt();

            ++ nOntologies;
            System.out.println("Writing ontology " + ontologyId + " (" + nOntologies + ")");

            jsonWriter.name("ontologyId");
            jsonWriter.value(ontologyId);



            jsonWriter.name(IMPORTS_FROM.getText());
            jsonWriter.beginArray();
            var imports = pass1Result.ontologyIdToImportedOntologyIds.get(ontologyId);
            if(imports != null) {
                for(String ontId : imports) {
                    jsonWriter.value(ontId);
                }
            }
            jsonWriter.endArray();


            jsonWriter.name(EXPORTS_TO.getText());
            jsonWriter.beginArray();
            var importedBy = pass1Result.ontologyIdToImportingOntologyIds.get(ontologyId);
            if(importedBy != null) {
                for(String ontId : importedBy) {
                    jsonWriter.value(ontId);
                }
            }
            jsonWriter.endArray();


            Set<String> ontologyGatheredStrings = new TreeSet<>();

            jsonWriter.name("classes");
            writeEntityArray(backendUrl,numberOfTerms,jsonWriter,"classes",ontologyId,leveldb,pass1Result);
            jsonWriter.name("properties");
            writeEntityArray(backendUrl,numberOfProperties,jsonWriter,"properties",ontologyId,leveldb,pass1Result);
            jsonWriter.name("individuals");
            writeEntityArray(backendUrl,numberOfIndividuals,jsonWriter,"individuals",ontologyId,leveldb,pass1Result);

            for (Map.Entry entry : ontology.getAsJsonObject().entrySet()){
                String key = entry.getKey().toString();
                jsonWriter.name(key);
                JsonElement ontologyGatheredStringsElement = ontology.getAsJsonObject().get(key);
                ontologyGatheredStrings.add(ExtractIriFromPropertyName.extract(key));
                System.out.println("key: "+key+" - value: " + ontologyGatheredStringsElement.toString());
                CopyJsonGatheringStringsFromService.copyJsonGatheringStrings(ontologyGatheredStringsElement, jsonWriter, ontologyGatheredStrings);
            }

            jsonWriter.name("linkedEntities");
            writeLinkedEntitiesFromGatheredStrings(jsonWriter, ontologyGatheredStrings, ontologyId, null, leveldb, pass1Result);
            jsonWriter.endObject();

        }
        jsonWriter.endArray();
        jsonWriter.endObject();
        jsonWriter.close();

        System.out.println("--- Linker Pass 2 complete");
    }

    private static void writeEntityArray(String backendUrl, int noofEntities, JsonWriter jsonWriter, String entityType, String ontologyId, LevelDB leveldb, LinkerPass1FromService.LinkerPass1Result pass1Result) throws IOException {
        JsonArray terms = getEntitiesAsJsonArray(backendUrl + "/api/v2/ontologies/" + ontologyId + "/" + entityType + "?size=" + noofEntities, null, "elements");

        jsonWriter.beginArray();

        for (JsonElement term : terms) {
            String entityIri = term.getAsJsonObject().get("iri").getAsString();
            Set<String> stringsInEntity = new HashSet<String>();
            jsonWriter.beginObject();
            for (Map.Entry entry : term.getAsJsonObject().entrySet()) {
                String name = entry.getKey().toString();
                String iri = entityIri;
                stringsInEntity.add(ExtractIriFromPropertyName.extract(name));
                jsonWriter.name(name);
                if (name.equals("iri")) {
                    entityIri = iri;
                    jsonWriter.value(entityIri);
                } else if (name.equalsIgnoreCase("curie")) {
                    JsonElement curieElement = term.getAsJsonObject().get(name);
                    processCurieObject(curieElement, jsonWriter, pass1Result, entityIri);
                } else if (name.equalsIgnoreCase("shortForm")) {
                    JsonElement shortFormElement = term.getAsJsonObject().get(name);
                    processShortFormObject(shortFormElement, jsonWriter, pass1Result, entityIri);
                } else {
                    JsonElement gatheringStringsElement = term.getAsJsonObject().get(name);
                    System.out.println("name: "+name+" - value: "+gatheringStringsElement.toString());
                    CopyJsonGatheringStringsFromService.copyJsonGatheringStrings(gatheringStringsElement, jsonWriter, stringsInEntity);
                }
            }

            EntityDefinitionSet defOfThisEntity = pass1Result.iriToDefinitions.get(entityIri);
            if (defOfThisEntity != null) {

                jsonWriter.name(IS_DEFINING_ONTOLOGY.getText());
                jsonWriter.value(defOfThisEntity.definingOntologyIds.contains(ontologyId));

                if (defOfThisEntity.definingDefinitions.size() > 0) {
                    jsonWriter.name(DEFINED_BY.getText());
                    jsonWriter.beginArray();
                    for (var def : defOfThisEntity.definingDefinitions) {
                        jsonWriter.value(def.ontologyId);
                    }
                    jsonWriter.endArray();
                }

                if (defOfThisEntity.definitions.size() > 0) {
                    jsonWriter.name(APPEARS_IN.getText());
                    jsonWriter.beginArray();
                    for (var def : defOfThisEntity.definitions) {
                        jsonWriter.value(def.ontologyId);
                    }
                    jsonWriter.endArray();
                }
            }

            jsonWriter.name("linkedEntities");
            writeLinkedEntitiesFromGatheredStrings(jsonWriter, stringsInEntity, ontologyId, entityIri, leveldb, pass1Result);

            jsonWriter.endObject();

        }
        jsonWriter.endArray();
    }


    private static void writeLinkedEntitiesFromGatheredStrings(JsonWriter jsonWriter, Set<String> strings, String ontologyId, String entityIri, LevelDB leveldb, LinkerPass1FromService.LinkerPass1Result pass1Result) throws IOException {

        jsonWriter.beginObject();

        for(String str : strings) {

            if(str.trim().length() == 0) {
                continue;
            }

            if(//str.startsWith("http://www.w3.org/2000/01/rdf-schema#") ||
                    str.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#") ||
                    //str.startsWith("http://www.geneontology.org/formats/oboInOwl#") ||
                    str.startsWith("http://www.w3.org/2002/07/owl#")) {
                continue;
            }

            if(entityIri != null && str.equals(entityIri)) {
                continue;
            }

            EntityDefinitionSet iriMapping = pass1Result.iriToDefinitions.get(str);

            if(iriMapping != null) {
                jsonWriter.name(str);
                jsonWriter.beginObject();
                writeIriMapping(jsonWriter, iriMapping, ontologyId);
                jsonWriter.endObject();
                continue;
            }

            // The string wasn't in any ontology. Maybe bioregistry can turn it into a curie?
            String curie = bioregistry.getCurieForUrl(str);

            if(curie == null) {
                // or maybe the string itself is a curie?
                if (str.matches("^[A-z0-9]+:[A-z0-9]+$")) {
                    curie = str;
                }
            }

            if (curie != null) {

                boolean foundCurieMatchToOntologyTerm = false;

                String databaseId = curie.substring(0, curie.indexOf(':'));
                String entryId = curie.substring(curie.indexOf(':') + 1);

                // The databaseId might be the preferredPrefix of an ontology in OLS
                Set<String> ontologyIds = pass1Result.preferredPrefixToOntologyIds.get(databaseId);
                if (ontologyIds != null) {
                    for (String curieOntologyId : ontologyIds) {
                        Set<String> ontologyBaseUris = pass1Result.ontologyIdToBaseUris
                                .get(curieOntologyId);
                        if (ontologyBaseUris != null) {
                            for (String ontologyBaseUri : ontologyBaseUris) {
                                String iri = ontologyBaseUri + entryId;
                                EntityDefinitionSet curieIriMapping = pass1Result.iriToDefinitions
                                        .get(iri);

                                if (curieIriMapping != null) {
                                    foundCurieMatchToOntologyTerm = true;
                                    jsonWriter.name(str);
                                    jsonWriter.beginObject();
                                    jsonWriter.name("iri");
                                    jsonWriter.value(iri);
                                    writeIriMapping(jsonWriter, curieIriMapping,
                                            ontologyId);
                                    break;
                                }
                            }

                            if(foundCurieMatchToOntologyTerm)
                                break;
                        }
                    }
                }

                CurieMapResult curieMapping = mapCurie(databaseId, entryId);

                if (curieMapping != null) {

                    // It was a CURIE which we were able to map to an URL
		    // using bioregistry.

                    if (!foundCurieMatchToOntologyTerm) {
                        jsonWriter.name(str);
                        jsonWriter.beginObject();
                    }

                    jsonWriter.name("url");
                    jsonWriter.value(curieMapping.url);
                    jsonWriter.name("source");
                    jsonWriter.value(curieMapping.source);
                    jsonWriter.name("curie");
                    jsonWriter.value(curie);

                    foundCurieMatchToOntologyTerm = true;
                }

                if (foundCurieMatchToOntologyTerm)
                    jsonWriter.endObject();

            }

        // No match as an IRI or as a CURIE. Look in LevelDB (for ORCIDs etc.)
            if(leveldb != null) {
                JsonElement leveldbMatch = leveldb.get(str);

                if(leveldbMatch != null) {
                    jsonWriter.name(str);
                    com.google.gson.internal.Streams.write(leveldbMatch, jsonWriter);
                    continue;
                }
            }


        }

        jsonWriter.endObject(); // linkedEntities
    }

    private static void writeIriMapping(JsonWriter jsonWriter, EntityDefinitionSet definitions, String ontologyId) throws IOException {

        if(definitions.definingDefinitions.size() > 0) {

	    // There are ontologies which canonically define this term

            jsonWriter.name(DEFINED_BY.getText());
            jsonWriter.beginArray();
            for(var def : definitions.definingDefinitions) {
                jsonWriter.value(def.ontologyId);
            }
            jsonWriter.endArray();

        }  else {

		// The term does not have any canonically defining ontologies...

		if(definitions.definingOntologyIds.size() == 1) {

			// ...and is only defined in ONE ontology. Therefore that ontology is the canonical defining ontology as far as OLS is concerned
			jsonWriter.name(DEFINED_BY.getText());
			jsonWriter.beginArray();
			jsonWriter.value(definitions.definingOntologyIds.iterator().next());
			jsonWriter.endArray();

		} else {

			// ...and is defined in multiple ontologies. We cannot establish a defining ontology.
		}

	}

	jsonWriter.name("numAppearsIn");
    jsonWriter.value(definitions.definitions.size());

	jsonWriter.name(HAS_LOCAL_DEFINITION.getText());
	jsonWriter.value(definitions.ontologyIdToDefinitions.containsKey(ontologyId));

        EntityDefinition defFromThisOntology = definitions.ontologyIdToDefinitions.get(ontologyId);

        // 1. Prefer metadata from the defining ontology
        if(definitions.definingDefinitions.size() > 0) {
	    EntityDefinition definingOntology = definitions.definingDefinitions.iterator().next();

	    jsonWriter.name("label");
	    com.google.gson.internal.Streams.write(definingOntology.label, jsonWriter);
        jsonWriter.name("curie");
        com.google.gson.internal.Streams.write(definingOntology.curie, jsonWriter);
	    jsonWriter.name("type");
        jsonWriter.beginArray();
        for(String type : definingOntology.entityTypes) {
            jsonWriter.value(type);
        }
        jsonWriter.endArray();

	// 2. Else look for metadata from this ontology
	} else if(defFromThisOntology != null) {

            jsonWriter.name("label");
            com.google.gson.internal.Streams.write(defFromThisOntology.label, jsonWriter);
            jsonWriter.name("curie");
            com.google.gson.internal.Streams.write(defFromThisOntology.curie, jsonWriter);
            jsonWriter.name("type");
            jsonWriter.beginArray();
            for(String type : defFromThisOntology.entityTypes) {
                jsonWriter.value(type);
            }
            jsonWriter.endArray();

	// 3. Fall back on the first ontology we encounter that defines the IRI
	//
	// This only applies if (a) the importing ontology didn't define the entity and (b) no other ontology
	// was considered canonical
	//
	} else {
            EntityDefinition fallbackDef = definitions.definitions.iterator().next();
            jsonWriter.name("type");
            jsonWriter.beginArray();
            for(String type : fallbackDef.entityTypes) {
                jsonWriter.value(type);
            }
            jsonWriter.endArray();
            jsonWriter.name("label");
            com.google.gson.internal.Streams.write(fallbackDef.label, jsonWriter);
            jsonWriter.name("curie");
            com.google.gson.internal.Streams.write(fallbackDef.curie, jsonWriter);
        }
    }

    private static CurieMapResult mapCurie(String databaseId, String entryId) {

	// check GO db-xrefs for an URL
	String url = dbUrls.getUrlForId(databaseId, entryId);

	if(url != null) {
		CurieMapResult res = new CurieMapResult();
		res.url = url;
		res.source = dbUrls.getXrefUrls();
		return res;
	}

	// check bioregistry for an URL
	url = bioregistry.getUrlForId(databaseId, entryId);

	if(url != null) {
		CurieMapResult res = new CurieMapResult();
		res.url = url;
		res.source = bioregistry.getRegistryUrl();
		return res;
	}

        return null;
    }

    private static class CurieMapResult {
        public String url;
        public String source;
    }

    private static void processShortFormObject(JsonElement shortFormElement, JsonWriter jsonWriter, LinkerPass1FromService.LinkerPass1Result pass1Result, String entityIri) throws IOException {
        JsonObject shortFormObject = new JsonObject();
        JsonArray typeArray = new JsonArray();
        typeArray.add("literal");
        shortFormObject.add("type", typeArray);
        String shortFormValue = shortFormElement.getAsString();
        // shortFormValue = getProcessedCurieValue(pass1Result, entityIri).replace(":", "_");
        shortFormObject.addProperty("value",shortFormValue);

        // Write the modified short form object
        jsonWriter.beginObject();
        jsonWriter.name("type");
        jsonWriter.beginArray();
        for (JsonElement typeElement : shortFormObject.getAsJsonArray("type")) {
            jsonWriter.value(typeElement.getAsString());
        }
        jsonWriter.endArray();
        jsonWriter.name("value").value(shortFormObject.get("value").getAsString());
        jsonWriter.endObject();
    }

    private static void processCurieObject(JsonElement curieElement, JsonWriter jsonWriter, LinkerPass1FromService.LinkerPass1Result pass1Result, String entityIri) throws IOException {
        JsonObject curieObject = new  JsonObject();
        JsonArray typeArray = new JsonArray();
        typeArray.add("literal");
        curieObject.add("type", typeArray);
        String curieValue = curieElement.getAsString();
        //curieValue = getProcessedCurieValue(pass1Result, entityIri);
        curieObject.addProperty("value", curieValue);

        // Write the modified curie object
        jsonWriter.beginObject();
        jsonWriter.name("type");
        jsonWriter.beginArray();
        for (JsonElement typeElement : curieObject.getAsJsonArray("type")) {
            jsonWriter.value(typeElement.getAsString());
        }
        jsonWriter.endArray();
        jsonWriter.name("value").value(curieObject.get("value").getAsString());
        jsonWriter.endObject();
    }

    private static String getProcessedCurieValue(LinkerPass1.LinkerPass1Result pass1Result, String entityIri) {
        var def = pass1Result.iriToDefinitions.get(entityIri);
        if (def!= null && def.definitions.iterator().hasNext()) {
            JsonObject defCurieObject = def.definitions.iterator().next().curie.getAsJsonObject();
            if (defCurieObject.has("value")) {
                return defCurieObject.get("value").getAsString();
            }
        }
        return "";
    }
}
