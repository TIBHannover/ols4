import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.regex.Pattern;

import static uk.ac.ebi.ols.shared.DefinedFields.*;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */

public class LinkerPass2FromServiceJSON extends ServiceBase {

    public static final OboDatabaseUrlService dbUrls = new OboDatabaseUrlService();
    public static final Bioregistry bioregistry = new Bioregistry();
    public static final String[] LINKER_KEYS = new String[] { "numAppearsIn", "linkedEntities", "importsFrom", "exportsTo", "definedBy", "appearsIn", "isDefiningOntology", "hasLocalDefinition" };

    public static void run(String backendUrl, int pageSize, String outputJsonFilename, LevelDB leveldb, LinkerPass1FromServiceJSON.LinkerPass1Result pass1Result) throws IOException {
        JsonArray ontologies = getEntitiesAsJsonArray(backendUrl+"/api/fulljson/entities?entity_type=ONTOLOGY&size=1000", null,"content");
        JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(new FileOutputStream(outputJsonFilename)));
        jsonWriter.setIndent("  ");

        System.out.println("--- Linker Pass 2: Processing " + backendUrl);
        int nOntologies = 0;

        jsonWriter.beginObject();
        jsonWriter.name("ontologies");
        jsonWriter.beginArray();
        for (JsonElement ont : ontologies){
            JsonObject ontology = jsonParser.parse(ont.getAsString()).getAsJsonObject();
            int numberOfTerms = 0;
            int numberOfProperties = 0;
            int numberOfIndividuals = 0;

            jsonWriter.beginObject();
            String ontologyId = ontology.getAsJsonObject().get("ontologyId").getAsString();

            numberOfTerms = ontology.getAsJsonObject().get("numberOfClasses").getAsJsonObject().get("value").getAsInt();
            numberOfProperties = ontology.getAsJsonObject().get("numberOfProperties").getAsJsonObject().get("value").getAsInt();
            numberOfIndividuals = ontology.getAsJsonObject().get("numberOfIndividuals").getAsJsonObject().get("value").getAsInt();

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
            writeEntityArray(backendUrl,numberOfTerms,pageSize,jsonWriter,"TERM",ontologyId, leveldb,pass1Result);
            jsonWriter.name("properties");
            writeEntityArray(backendUrl,numberOfProperties, pageSize, jsonWriter,"PROPERTY",ontologyId, leveldb,pass1Result);
            jsonWriter.name("individuals");
            writeEntityArray(backendUrl,numberOfIndividuals, pageSize, jsonWriter,"INDIVIDUAL",ontologyId, leveldb,pass1Result);

            for (Map.Entry<String, JsonElement> entry : ontology.entrySet()){
                if (List.of(LINKER_KEYS).contains(entry.getKey()))
                    continue;
                jsonWriter.name(entry.getKey());
                extractGatheredStrings(entry,ontologyGatheredStrings);
                CopyJsonGatheringStringsFromService.copyJsonGatheringStrings(entry.getValue(), jsonWriter, ontologyGatheredStrings);
            }

            jsonWriter.name("linkedEntities");
            filter(ontologyGatheredStrings,null);
            String le = "Ontology linkedEntities: ontologyGatheredStrings=" + ontologyGatheredStrings+" ontologyId=" + ontologyId+" leveldb: "+leveldb;
            //System.out.println("Ontology ID: "+ontologyId+" - NoofChars: "+le.length());
            System.out.println(le);
            writeLinkedEntitiesFromGatheredStrings(jsonWriter, ontologyGatheredStrings, ontologyId, null, leveldb, pass1Result);
            jsonWriter.endObject();

        }
        jsonWriter.endArray();
        jsonWriter.endObject();
        jsonWriter.close();

        System.out.println("--- Linker Pass 2 complete");
    }

    private static void extractGatheredStrings(Map.Entry<String, JsonElement> entry, Set<String> ontologyGatheredStrings){
        // skip the curies, curie like strings (AEON_0000026), ORCID numbers in curie form or only in number form, etc
        String key = entry.getKey();
        JsonElement value = entry.getValue();
        ontologyGatheredStrings.add(ExtractIriFromPropertyName.extract(key));
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            ontologyGatheredStrings.add(ExtractIriFromPropertyName.extract(value.getAsString()));
        } else if (value instanceof JsonArray) {
            for (JsonElement elem :value.getAsJsonArray()){
                extractGatheredStrings(new AbstractMap.SimpleEntry<String, JsonElement>(key, elem), ontologyGatheredStrings);
            }
        } else if (value instanceof JsonObject) {
            for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet()){
                extractGatheredStrings(e, ontologyGatheredStrings);
            }
        }
    }

    // Updated pattern:
    // 1. Prefix must be letters, digits, underscore, hyphen, or dot.
    // 2. Must contain a single colon.
    // 3. Must NOT start with URI schemes like http:, https:, ftp:, urn:, etc.
    private static final Pattern CURIE_PATTERN = Pattern.compile(
            "^(?!https?:)(?!ftp:)(?!urn:)(?!file:)(?!mailto:)([A-Za-z][A-Za-z0-9_.-]*)([:_][A-Za-z0-9_.-]+)+$"
    );

    // 3️⃣ ORCID-like pattern (e.g., 0000-0002-1595-3213)
    private static final Pattern ORCID_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]$"
    );

    /**
     * Checks whether a string is a valid CURIE (Compact URI), not a full URI.
     * Examples: foaf:Person, AEON:0000084, GO:0008150
     */
    public static boolean isCURIE(String input) {
        if (input == null) return false;
        return CURIE_PATTERN.matcher(input).matches();
    }

    public static boolean isORCID(String input) {
        if (input == null) return false;
        return ORCID_PATTERN.matcher(input).matches();
    }

    public static void filter(Set<String> ontologyGatheredStrings, String iri) {
        Set<String> filtered = new HashSet<>(ontologyGatheredStrings);
        if (iri == null) {
            for (String s : filtered) {
                for (String linkerKey : LINKER_KEYS) {
                    if (s.equals(linkerKey) )
                        ontologyGatheredStrings.remove(s);
                }
                if (isCURIE(s) || isORCID(s)) ontologyGatheredStrings.remove(s);
            }
        } else {
            String curieWithTag = iri.split("/")[iri.split("/").length - 1];
            String curie = curieWithTag.split("#")[curieWithTag.split("#").length - 1];
            String[] curieComponents = curie.split("_");
            String modifiedCurie = StringUtils.join(curieComponents,"_");
            for (String s : filtered) {
                for (String linkerKey : LINKER_KEYS) {
                    if (s.equals(linkerKey) )
                        ontologyGatheredStrings.remove(s);
                }
                if (isCURIE(s)){
                    if (s.replace(":","_").equalsIgnoreCase(modifiedCurie))
                        ontologyGatheredStrings.remove(s);
                } else if (isORCID(s)) ontologyGatheredStrings.remove(s);
            }
        }

    }

    private static void writeEntityArray(String backendUrl, int noofEntities, int pageSize, JsonWriter jsonWriter, String entityType, String ontologyId, LevelDB leveldb, LinkerPass1FromServiceJSON.LinkerPass1Result pass1Result) throws IOException {
        jsonWriter.beginArray();
        for (int i = 0; i<numberOfPages(noofEntities, pageSize); i++){
            JsonArray terms = getEntitiesAsJsonArray(backendUrl+"/api/fulljson/entities?onto="+ontologyId+"&entity_type="+entityType+"&size="+pageSize+"&page="+i, null,"content");
            for (JsonElement term : terms) {
                JsonObject entity = jsonParser.parse(term.getAsString()).getAsJsonObject();
                String entityIri = entity.get("iri").getAsString();
                String shortForm = extractShortFormFromAllOntologies(pass1Result.ontologyIdToBaseUris,pass1Result.preferredPrefixToOntologyIds, entityIri);
                String extractedCurie = extractCurieFromAllOntologies(shortForm,pass1Result.preferredPrefixToOntologyIds);

                System.out.println("ontologyId: "+ontologyId+" entityIri: "+entityIri+" shortForm: "+shortForm+" extractedCurie: "+extractedCurie);
                Set<String> stringsInEntity = new HashSet<String>();
                jsonWriter.beginObject();
                String curie = "none";

                for (Map.Entry<String, JsonElement> entry : entity.entrySet()){
                    String name = entry.getKey();
                    if (name.equals("iri")) {
                        extractGatheredStrings(entry, stringsInEntity);
                        stringsInEntity.remove(entityIri);
                        jsonWriter.name(name);
                        jsonWriter.value(entityIri);
                    } else if (name.equalsIgnoreCase("curie")) {
                        extractGatheredStrings(entry, stringsInEntity);
                        jsonWriter.name(name);
                        JsonElement curieElement = entity.get(name);
                        curieElement.getAsJsonObject().addProperty("value", extractedCurie);
                        curie = curieElement.getAsJsonObject().get("value").getAsString();
                        stringsInEntity.remove(curie);
                        com.google.gson.internal.Streams.write(curieElement, jsonWriter);
                    } else if (name.equalsIgnoreCase("shortForm")) {
                        extractGatheredStrings(entry, stringsInEntity);
                        jsonWriter.name(name);
                        JsonElement shortFormElement = entity.get(name);
                        shortFormElement.getAsJsonObject().addProperty("value", shortForm);
                        stringsInEntity.remove(shortFormElement.getAsJsonObject().get("value").getAsString());
                        com.google.gson.internal.Streams.write(shortFormElement, jsonWriter);
                    }
                }

                for (Map.Entry<String, JsonElement> entry : entity.entrySet()) {
                    String name = entry.getKey();

                    if (name.equals("iri") || name.equalsIgnoreCase("curie") || name.equalsIgnoreCase("shortForm")) {
                        continue;
                    } else if (List.of(LINKER_KEYS).contains(name)) {
                        continue;
                    } else {
                        extractGatheredStrings(entry, stringsInEntity);
                        jsonWriter.name(name);
                        JsonElement gatheringStringsElement = entity.get(name);
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

                filter(stringsInEntity,entityIri);
                for (Map.Entry<String, JsonElement> entry : entity.get("linkedEntities").getAsJsonObject().entrySet())
                    if (entry.getKey().equals(curie))
                        stringsInEntity.add(curie);
                jsonWriter.name("linkedEntities");
                String le = "Entity linkedEntities: ontologyGatheredStrings=" + stringsInEntity+" ontologyId=" + ontologyId+" entityIri: "+entityIri+" leveldb: "+leveldb;
                System.out.println("Ontology Id: "+ontologyId+" Entity IRI: "+entityIri+" - NoofChars: "+le.length());
                System.out.println(le);
                writeLinkedEntitiesFromGatheredStrings(jsonWriter, stringsInEntity, ontologyId, entityIri, leveldb, pass1Result);

                jsonWriter.endObject();

            }
        }
        jsonWriter.endArray();
    }


    private static void writeLinkedEntitiesFromGatheredStrings(JsonWriter jsonWriter, Set<String> strings, String ontologyId, String entityIri, LevelDB leveldb, LinkerPass1FromServiceJSON.LinkerPass1Result pass1Result) throws IOException {

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
}
