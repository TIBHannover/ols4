import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CopyJsonGatheringStringsFromService {

    private static final Pattern curiePattern = Pattern.compile("[A-Z]+:[0-9A-z]+");
    private static final Pattern uriPattern = Pattern.compile("[A-z]+:\\/\\/[^\\s]+");

    public static void copyJsonGatheringStrings(JsonElement gatheringStringsElement, JsonWriter jsonWriter, Set<String> gatheredStrings) throws IOException {

        if (gatheringStringsElement.isJsonObject()) {
            copyObject(gatheringStringsElement, jsonWriter, gatheredStrings);
        } else if (gatheringStringsElement.isJsonArray()) {
            copyArray(gatheringStringsElement, jsonWriter, gatheredStrings);
        } else if (gatheringStringsElement.isJsonPrimitive() && gatheringStringsElement.getAsJsonPrimitive().isString()) {
            String str = gatheringStringsElement.getAsString();
            gatheredStrings.add(str);

            Matcher matcher = curiePattern.matcher(str);

            while (matcher.find()) {
                gatheredStrings.add(matcher.group());
            }

            Matcher uriMatcher = uriPattern.matcher(str);

            while (uriMatcher.find()) {
                gatheredStrings.add(uriMatcher.group());
            }

            jsonWriter.value(str);
        } else {
            com.google.gson.internal.Streams.write(gatheringStringsElement, jsonWriter);
        }
    }

    public static void copyObject(JsonElement gatheringStringsElement, JsonWriter jsonWriter, Set<String> gatheredStrings) throws IOException {
        jsonWriter.beginObject();
        for (Map.Entry<String, JsonElement> entry : gatheringStringsElement.getAsJsonObject().entrySet()){
            String name = entry.getKey();
            gatheredStrings.add(ExtractIriFromPropertyName.extract(name));
            jsonWriter.name(name);
            copyJsonGatheringStrings(entry.getValue(), jsonWriter, gatheredStrings);
        }
        jsonWriter.endObject();
    }

    public static void copyArray(JsonElement element, JsonWriter jsonWriter, Set<String> gatheredStrings) throws IOException {
        jsonWriter.beginArray();

        for (JsonElement jsonElement : element.getAsJsonArray()) {
            copyJsonGatheringStrings(jsonElement, jsonWriter, gatheredStrings);
        }

        jsonWriter.endArray();
    }
}
