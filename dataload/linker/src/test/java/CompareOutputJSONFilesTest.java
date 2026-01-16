import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.internal.Diff;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

public class CompareOutputJSONFilesTest {

    protected static final JsonParser jsonParser = new JsonParser();
    static JsonElement element1 = new JsonNull();
    static JsonElement element2 = new JsonNull();

    @BeforeAll
    static void init(){
        try {
            Path file = Miscallenous.resolveConfigFile(System.getProperty("fileOutputPath"));
            Path service = Miscallenous.resolveConfigFile(System.getProperty("serviceOutputPath"));
            if(Files.exists(file) && Files.isRegularFile(file)){
                FileInputStream is1 = new FileInputStream(file.toFile());
                InputStreamReader reader1 = new InputStreamReader(is1);
                JsonReader jsonReader1 = new JsonReader(reader1);
                element1 = jsonParser.parse(jsonReader1);
            }
            if(Files.exists(service) && Files.isRegularFile(service)){
                FileInputStream is2 = new FileInputStream(service.toFile());
                InputStreamReader reader2 = new InputStreamReader(is2);
                JsonReader jsonReader2 = new JsonReader(reader2);
                element2 = jsonParser.parse(jsonReader2);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void compareParticularEntityMetadata() {

        if (element1.isJsonNull() || element2.isJsonNull())
            return;

        JsonObject ont1 = new JsonObject();
        JsonObject ont2 = new JsonObject();
        String ontologyId = "s4wear";
        String entityType = "classes";

        for (JsonElement ontology : element1.getAsJsonObject().get("ontologies").getAsJsonArray())
            if (ontology.getAsJsonObject().get("ontologyId").getAsString().equals(ontologyId))
                ont1 = ontology.getAsJsonObject();

        for (JsonElement ontology : element2.getAsJsonObject().get("ontologies").getAsJsonArray())
            if (ontology.getAsJsonObject().get("ontologyId").getAsString().equals(ontologyId))
                ont2 = ontology.getAsJsonObject();

        for (JsonElement el1 : ont1.get(entityType).getAsJsonArray()){
            for (JsonElement el2 : ont2.get(entityType).getAsJsonArray()){
                if (el1.getAsJsonObject().get("iri").getAsString().equals(el2.getAsJsonObject().get("iri").getAsString())){
                    for (Map.Entry<String, JsonElement> entry :el1.getAsJsonObject().entrySet()){
                        //if(entry.getKey().equals("linkedEntities")){
                            System.out.println("IRI: "+el1.getAsJsonObject().get("iri").getAsString());
                            System.out.println("key: "+entry.getKey());
                            Diff diff = Diff.create(entry.getValue().toString(), el2.getAsJsonObject().get(entry.getKey()).toString(), "IRI: "+el1.getAsJsonObject().get("iri").getAsString()+" key: "+entry.getKey(), "",Configuration.empty().withOptions(IGNORING_ARRAY_ORDER).withTolerance(0.0d));
                            if (!diff.differences().contains("JSON documents have the same value."))
                                System.out.println(diff);
                             //assertJsonEquals(entry.getValue().toString(), el2.getAsJsonObject().get(entry.getKey()).toString(), Configuration.empty().withOptions(IGNORING_ARRAY_ORDER).withTolerance(0.0d));
                            //assertEquals(entry.getValue(), el2.getAsJsonObject().get(entry.getKey()));
                        //}

                    }

                }
            }
        }

        /*for (Map.Entry<String, JsonElement> entry : bao1.entrySet()){
            JsonElement value2 = bao2.get(entry.getKey());
            if (value2.isJsonObject() && entry.getValue().isJsonObject())
                if (value2.getAsJsonObject().has("value") && entry.getValue().getAsJsonObject().has("value"))
                    if (isTimestamp(value2.getAsJsonObject().get("value").getAsString()) && isTimestamp(entry.getValue().getAsJsonObject().get("value").getAsString()))
                        continue;
            assertEquals(entry.getValue(), value2);
        }*/



    }

    @Test
    void compareAllEntityMetadata(){
        if (element1.isJsonNull() || element2.isJsonNull())
            return;

        for (JsonElement ontology1 : element1.getAsJsonObject().get("ontologies").getAsJsonArray()){
            for (JsonElement ontology2 : element1.getAsJsonObject().get("ontologies").getAsJsonArray()){
                if (ontology1.getAsJsonObject().get("ontologyId").getAsString().equals(ontology2.getAsJsonObject().get("ontologyId").getAsString())){
                    System.out.println("Ontology::::: "+ontology1.getAsJsonObject().get("ontologyId").getAsString());
                    System.out.println("Ontology Classes:::: "+ontology1.getAsJsonObject().get("ontologyId").getAsString());
                    for (JsonElement el1 : ontology1.getAsJsonObject().get("classes").getAsJsonArray()){
                        for (JsonElement el2 : ontology2.getAsJsonObject().get("classes").getAsJsonArray()){
                            if (el1.getAsJsonObject().get("iri").getAsString().equals(el2.getAsJsonObject().get("iri").getAsString())){
                                System.out.println("IRI::: "+el1.getAsJsonObject().get("iri").getAsString());
                                for (Map.Entry<String, JsonElement> entry :el1.getAsJsonObject().entrySet()){
                                    if(!entry.getKey().equals("curie")  && !entry.getKey().equals("shortForm")){
                                        System.out.println("key: "+entry.getKey());
                                        assertJsonEquals(entry.getValue().toString(), el2.getAsJsonObject().get(entry.getKey()).toString(), Configuration.empty().withOptions(IGNORING_ARRAY_ORDER).withTolerance(0.0d));
                                    }

                                }

                            }
                        }
                    }

                    System.out.println("Ontology Properties:::: "+ontology1.getAsJsonObject().get("ontologyId").getAsString());
                    for (JsonElement el1 : ontology1.getAsJsonObject().get("properties").getAsJsonArray()){
                        for (JsonElement el2 : ontology2.getAsJsonObject().get("properties").getAsJsonArray()){
                            if (el1.getAsJsonObject().get("iri").getAsString().equals(el2.getAsJsonObject().get("iri").getAsString())){
                                System.out.println("IRI::: "+el1.getAsJsonObject().get("iri").getAsString());
                                for (Map.Entry<String, JsonElement> entry :el1.getAsJsonObject().entrySet()){
                                    if(!entry.getKey().equals("curie")  && !entry.getKey().equals("shortForm")){
                                        System.out.println("key: "+entry.getKey());
                                        assertJsonEquals(entry.getValue().toString(), el2.getAsJsonObject().get(entry.getKey()).toString(), Configuration.empty().withOptions(IGNORING_ARRAY_ORDER).withTolerance(0.0d));
                                    }

                                }

                            }
                        }
                    }

                    System.out.println("Ontology Individuals:::: "+ontology1.getAsJsonObject().get("ontologyId").getAsString());
                    for (JsonElement el1 : ontology1.getAsJsonObject().get("individuals").getAsJsonArray()){
                        for (JsonElement el2 : ontology2.getAsJsonObject().get("individuals").getAsJsonArray()){
                            if (el1.getAsJsonObject().get("iri").getAsString().equals(el2.getAsJsonObject().get("iri").getAsString())){
                                System.out.println("IRI::: "+el1.getAsJsonObject().get("iri").getAsString());
                                for (Map.Entry<String, JsonElement> entry :el1.getAsJsonObject().entrySet()){
                                    if(!entry.getKey().equals("curie")  && !entry.getKey().equals("shortForm")){
                                        System.out.println("key: "+entry.getKey());
                                        assertJsonEquals(entry.getValue().toString(), el2.getAsJsonObject().get(entry.getKey()).toString(), Configuration.empty().withOptions(IGNORING_ARRAY_ORDER).withTolerance(0.0d));
                                    }

                                }

                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean isTimestamp(String input) {
        try {
            // ISO-8601 timestamps are usually parseable by default
            LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }


}
