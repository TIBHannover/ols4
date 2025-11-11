import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void compareOntologyMetadata() {

        if (element1.isJsonNull() || element2.isJsonNull())
            return;

        JsonObject bao1 = new JsonObject();
        JsonObject bao2 = new JsonObject();

        for (JsonElement ontology : element1.getAsJsonObject().get("ontologies").getAsJsonArray())
            if (ontology.getAsJsonObject().get("ontologyId").getAsString().equals("bao"))
                bao1 = ontology.getAsJsonObject();

        for (JsonElement ontology : element2.getAsJsonObject().get("ontologies").getAsJsonArray())
            if (ontology.getAsJsonObject().get("ontologyId").getAsString().equals("bao"))
                bao2 = ontology.getAsJsonObject();

        for (JsonElement el1 : bao1.get("classes").getAsJsonArray()){
            for (JsonElement el2 : bao2.get("classes").getAsJsonArray()){
                if (el1.getAsJsonObject().get("iri").getAsString().equals(el2.getAsJsonObject().get("iri").getAsString())){
                    for (Map.Entry<String, JsonElement> entry :el1.getAsJsonObject().entrySet()){
                        if(entry.getKey().equals("linkedEntities")){
                            System.out.println("IRI: "+el1.getAsJsonObject().get("iri").getAsString());
                            System.out.println("key: "+entry.getKey());
                            assertEquals(entry.getValue(), el2.getAsJsonObject().get(entry.getKey()));
                        }

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
