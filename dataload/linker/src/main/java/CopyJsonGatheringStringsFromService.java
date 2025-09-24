import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
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

        for (Map.Entry<String, JsonElement> entry : gatheringStringsElement.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonArray()) {

                jsonWriter.beginArray();

                for (JsonElement jsonElement : entry.getValue().getAsJsonArray()) {
                    copyJsonGatheringStrings(jsonElement, jsonWriter, gatheredStrings);
                }

                jsonWriter.endArray();

            } else if (entry.getValue().isJsonObject()) {

                jsonWriter.beginObject();
                copyJsonGatheringStrings(entry.getValue().getAsJsonObject(), jsonWriter, gatheredStrings);
                jsonWriter.endObject();

            } else if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                String str = entry.getValue().getAsString();
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
    }
}
