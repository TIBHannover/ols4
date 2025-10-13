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
}
