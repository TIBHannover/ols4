import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ServiceBase {

    protected static final JsonParser jsonParser = new JsonParser();
	protected static CloseableHttpClient httpclient = HttpClients.createDefault();
	// Create a custom response handler
	protected static ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

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
        httpget.addHeader("caller", "TS_RELINK");
		System.out.println("Executing request " + httpget.getRequestLine());
		String responseBody = httpclient.execute(httpget, responseHandler);
		System.out.println("----------------------------------------");

		JsonElement jElement = jsonParser.parse(responseBody);

		if (property == null)
			return jElement.getAsJsonObject().getAsJsonArray(arrayName); // for v2 calls
		else
			return jElement.getAsJsonObject().getAsJsonObject(property).getAsJsonArray(arrayName); // for v1 calls

	}

    public static int numberOfPages(int noofEntities,int pageSize) {
        if  (noofEntities % pageSize == 0)
            return noofEntities/pageSize;
        else
            return (noofEntities/pageSize) + 1;
    }



    public static String extractShortForm(Set<String> ontologyBaseUris, String preferredPrefix,
                                           String uri) {

        if (uri.startsWith("urn:")) {
            return uri.substring(4);
        }

        // if(uri.startsWith("http://purl.obolibrary.org/obo/")) {
        // return uri.substring("http://purl.obolibrary.org/obo/".length());
        // }

        for (String baseUri : ontologyBaseUris) {
            if (uri.startsWith(baseUri) && preferredPrefix != null) {
                return preferredPrefix + "_" + uri.substring(baseUri.length());
            }
        }

        if (uri.contains("/") || uri.contains("#")) {

            return uri.substring(
                    Math.max(
                            uri.lastIndexOf('/'),
                            uri.lastIndexOf('#')) + 1);

        } else {

            return uri;
        }
    }

    public static String extractShortFormFromAllOntologies(Map<String, Set<String>> ontologyIdToBaseUris, Map<String, Set<String>> preferredPrefixToOntologyId, String uri){

        if (uri.startsWith("urn:")) {
            return uri.substring(4);
        }

        for (Map.Entry<String, Set<String>> entry : ontologyIdToBaseUris.entrySet()){
            String ontologyId = entry.getKey();
            for (String baseUri : entry.getValue()){
                if (uri.startsWith(baseUri)){
                    for (Map.Entry<String, Set<String>> ptoid : preferredPrefixToOntologyId.entrySet()){
                        if(ptoid.getValue().contains(ontologyId)){
                            return ptoid.getKey() + "_" + uri.substring(baseUri.length());
                        }
                    }
                }
            }
        }

        if (uri.contains("/") || uri.contains("#")) {

            return uri.substring(
                    Math.max(
                            uri.lastIndexOf('/'),
                            uri.lastIndexOf('#')) + 1);

        } else {

            return uri;
        }
    }

    public static String extractCurie(String shortForm, String preferredPrefix) {
        String curie;
        // Pattern for: single underscore, prefix matches preferredPrefix
        String preferredPrefixPattern = "^(?:" + Pattern.quote(preferredPrefix) + ")_([^_]+)$";
        // Pattern for: single underscore, suffix is all digits
        String singleUnderscoreDigitsPattern = "^[^_]+_(\\d+)$";
        // Pattern for: multiple underscores, suffix is all digits
        String multipleUnderscoresDigitsPattern = "^(.*)_(\\d+)$";
        if (shortForm.matches(preferredPrefixPattern)) {
            curie = shortForm.replaceFirst("_", ":");
        } else if (shortForm.matches(singleUnderscoreDigitsPattern)) {
            curie = shortForm.replaceFirst("_", ":");
        } else if (shortForm.matches(multipleUnderscoresDigitsPattern)) {
            // Multiple underscores, suffix is digits
            // Replace the last underscore with a colon
            curie = shortForm.replaceFirst("_(?=\\d+$)", ":");
        } else {
            // No transformation needed
            curie = shortForm;
        }
        return curie;
    }

    public static String extractCurieFromAllOntologies(String shortForm, Map<String, Set<String>> preferredPrefixToOntologyId){

        for (Map.Entry<String, Set<String>> ptoid : preferredPrefixToOntologyId.entrySet()){

            String preferredPrefixPattern = "^(?:" + Pattern.quote(ptoid.getKey()) + ")_([^_]+)$";
            // Pattern for: single underscore, suffix is all digits
            String singleUnderscoreDigitsPattern = "^[^_]+_(\\d+)$";
            // Pattern for: multiple underscores, suffix is all digits
            String multipleUnderscoresDigitsPattern = "^(.*)_(\\d+)$";
            if (shortForm.matches(preferredPrefixPattern)) {
                return shortForm.replaceFirst("_", ":");
            } else if (shortForm.matches(singleUnderscoreDigitsPattern)) {
                return shortForm.replaceFirst("_", ":");
            } else if (shortForm.matches(multipleUnderscoresDigitsPattern)) {
                // Multiple underscores, suffix is digits
                // Replace the last underscore with a colon
                return shortForm.replaceFirst("_(?=\\d+$)", ":");
            }
        }
        return shortForm;
    }
}
