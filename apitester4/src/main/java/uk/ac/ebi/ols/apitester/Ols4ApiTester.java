
package uk.ac.ebi.ols.apitester;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.google.gson.*;
import org.apache.commons.io.IOUtils;


public class Ols4ApiTester {


	Gson gson;
	String url, outDir;

	public Ols4ApiTester(String url, String outDir) {

		gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

		if(url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		this.url = url;
		this.outDir = outDir;
	}

	public boolean test() throws MalformedURLException, IOException {

		System.out.println("Waiting for API to become available...");

		JsonElement ontologies = null;

		int MAX_RETRIES = 60;

		for(int nRetries = 0; nRetries < MAX_RETRIES; ++ nRetries) {

			ontologies = getAll(url + "/api/ontologies");
			write(outDir + "/ontologies.json", ontologies);

			if(!ontologies.isJsonArray()) {
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {}

				continue;
			}
		}

		if(ontologies == null || !ontologies.isJsonArray()) {
			System.out.println("No ontologies returned! :-(");
			return false;
		} else {
			System.out.println("Got " + ontologies.getAsJsonArray().size() + " ontologies");
		}

		JsonElement v2Ontologies = getAll(url + "/api/v2/ontologies");
		write(outDir + "/v2/ontologies.json", v2Ontologies);

		List<String> ontologyIds = new ArrayList();
		for(JsonElement ontology : ontologies.getAsJsonArray()) {
			ontologyIds.add(ontology.getAsJsonObject().get("ontologyId").getAsString());
		}

		for(String ontologyId : ontologyIds) {

			/// v1

			JsonElement classes = getAll(url + "/api/ontologies/" + ontologyId + "/terms");
			write(outDir + "/ontologies/" + ontologyId + "/terms.json", classes);

			JsonElement properties = getAll(url + "/api/ontologies/" + ontologyId + "/properties");
			write(outDir + "/ontologies/" + ontologyId + "/properties.json", properties);

			JsonElement individuals = getAll(url + "/api/ontologies/" + ontologyId + "/individuals");
			write(outDir + "/ontologies/" + ontologyId + "/individuals.json", individuals);

			for(JsonElement _class : classes.getAsJsonArray()) {

				String iri = _class.getAsJsonObject().get("iri").getAsString();
				String doubleEncodedIri = doubleEncode(iri);

				JsonElement classJson = get(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri);
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + ".json", classJson);

				JsonElement parentsJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/parents");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/parents.json", parentsJson);

				JsonElement ancestorsJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/ancestors");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/ancestors.json", ancestorsJson);

				JsonElement hierarchicalParentsJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalParents");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalParents.json", hierarchicalParentsJson);

				JsonElement hierarchicalAncestorsJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalAncestors");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalAncestors.json", hierarchicalAncestorsJson);

				JsonElement childrenJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/children");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/children.json", childrenJson);

				JsonElement descendantsJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/descendants");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/descendants.json", descendantsJson);

				JsonElement hierarchicalChildrenJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalChildren");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalChildren.json", hierarchicalChildrenJson);

				JsonElement hierarchicalDescendantsJson = getAll(url + "/api/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalDescendants");
				write(outDir + "/ontologies/" + ontologyId + "/terms/" + doubleEncodedIri + "/hierarchicalDescendants.json", hierarchicalDescendantsJson);
			}

			for(JsonElement property : properties.getAsJsonArray()) {

				String iri = property.getAsJsonObject().get("iri").getAsString();
				String doubleEncodedIri = doubleEncode(iri);

				JsonElement propertyJson = get(url + "/api/ontologies/" + ontologyId + "/properties/" + doubleEncodedIri);
				write(outDir + "/ontologies/" + ontologyId + "/properties/" + doubleEncodedIri + ".json", propertyJson);

				// TODO
			}

			for(JsonElement individual : individuals.getAsJsonArray()) {

				String iri = individual.getAsJsonObject().get("iri").getAsString();
				String doubleEncodedIri = doubleEncode(iri);

				JsonElement individualJson = get(url + "/api/ontologies/" + ontologyId + "/individuals/" + doubleEncodedIri);
				write(outDir + "/ontologies/" + ontologyId + "/individuals/" + doubleEncodedIri + ".json", individualJson);

				// TODO
			}




			/// v2

			JsonElement v2Entities = getAll(url + "/api/v2/ontologies/" + ontologyId + "/entities");
			write(outDir + "/v2/ontologies/" + ontologyId + "/entities.json", v2Entities);

			JsonElement v2Classes = getAll(url + "/api/v2/ontologies/" + ontologyId + "/classes");
			write(outDir + "/v2/ontologies/" + ontologyId + "/classes.json", v2Classes);

			JsonElement v2Properties = getAll(url + "/api/v2/ontologies/" + ontologyId + "/properties");
			write(outDir + "/v2/ontologies/" + ontologyId + "/properties.json", v2Properties);

			JsonElement v2Individuals = getAll(url + "/api/v2/ontologies/" + ontologyId + "/individuals");
			write(outDir + "/v2/ontologies/" + ontologyId + "/individuals.json", v2Individuals);


			for(JsonElement v2Entity : v2Entities.getAsJsonArray()) {

				String iri = v2Entity.getAsJsonObject().get("iri").getAsString();
				String doubleEncodedIri = doubleEncode(iri);

				JsonElement entityJson = get(url + "/api/ontologies/" + ontologyId + "/entities/" + doubleEncodedIri);
				write(outDir + "/ontologies/" + ontologyId + "/entities/" + doubleEncodedIri + ".json", entityJson);

				// TODO
			}

			for(JsonElement v2Class : v2Classes.getAsJsonArray()) {

				String iri = v2Class.getAsJsonObject().get("iri").getAsString();
				String doubleEncodedIri = doubleEncode(iri);

				JsonElement classJson = get(url + "/api/ontologies/" + ontologyId + "/classes/" + doubleEncodedIri);
				write(outDir + "/ontologies/" + ontologyId + "/classes/" + doubleEncodedIri + ".json", classJson);

				// TODO
			}

			for(JsonElement v2Property : v2Properties.getAsJsonArray()) {

				String iri = v2Property.getAsJsonObject().get("iri").getAsString();
				String doubleEncodedIri = doubleEncode(iri);

				JsonElement propertyJson = get(url + "/api/ontologies/" + ontologyId + "/properties/" + doubleEncodedIri);
				write(outDir + "/ontologies/" + ontologyId + "/properties/" + doubleEncodedIri + ".json", propertyJson);

				// TODO
			}

			for(JsonElement v2Individual : v2Individuals.getAsJsonArray()) {

				String iri = v2Individual.getAsJsonObject().get("iri").getAsString();
				String doubleEncodedIri = doubleEncode(iri);

				JsonElement individualJson = get(url + "/api/ontologies/" + ontologyId + "/individuals/" + doubleEncodedIri);
				write(outDir + "/ontologies/" + ontologyId + "/individuals/" + doubleEncodedIri + ".json", individualJson);

				// TODO
			}
		}

		return true;

	}

	public void write(String path, JsonElement element) throws FileNotFoundException, IOException {

		Files.createDirectories(  Paths.get(path).toAbsolutePath().getParent() );

		File file = new File(path);

		FileOutputStream os = new FileOutputStream(file);

		try {
			os.write( gson.toJson(element).getBytes());
		} finally {
			os.close();
		}
	}

	public JsonElement getAll(String url) {

		try {
			JsonArray allEntries = new JsonArray();

			for(JsonObject res = get(url).getAsJsonObject();;) {

				if(res.has("error")) {
					return res;
				}

				JsonElement embedded = res.get("_embedded");

				if(embedded == null) {
					break;
				}

				String resourceName = embedded.getAsJsonObject().keySet().iterator().next();
				JsonArray entries = embedded.getAsJsonObject().get(resourceName).getAsJsonArray();
				allEntries.addAll(entries);

				JsonObject links = res.get("_links").getAsJsonObject();

				JsonElement nextObj = links.get("next");

				if(nextObj == null) {
					break;
				}

				String next = nextObj.getAsJsonObject().get("href").getAsString();

				res = get(next).getAsJsonObject();
			}

			return deepSort(removeDates(normalizeURLs(allEntries))).getAsJsonArray();

		} catch(Exception e) {
			return gson.toJsonTree(e);
		}
	}

	public JsonElement get(String url) throws IOException {

		System.out.println("GET " + url);

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

		if (100 <= conn.getResponseCode() && conn.getResponseCode() <= 399) {
			InputStream is = conn.getInputStream();
			Reader reader = new InputStreamReader(is, "UTF-8");
			JsonElement result = JsonParser.parseReader(reader);
			return result;
		} else {
			InputStream is = conn.getErrorStream();
			Reader reader = new InputStreamReader(is, "UTF-8");
			JsonObject error = new JsonObject();
			error.addProperty("error", IOUtils.toString(is, StandardCharsets.UTF_8));
			return error;
		}
	}

	public JsonElement normalizeURLs(JsonElement element) {

		if(element.isJsonArray()) {

			JsonArray arr = element.getAsJsonArray();
			JsonArray res = new JsonArray();
			
			for(int i = 0; i < arr.size(); ++ i) {
				res.add(normalizeURLs(arr.get(i)));
			}

			return res;

		} else if(element.isJsonObject()) {

			JsonObject obj = element.getAsJsonObject();
			JsonObject res = new JsonObject();

			for(Entry<String, JsonElement> entry : obj.entrySet()) {
				res.add(entry.getKey(), normalizeURLs(entry.getValue()));
			}

			return res;

		} else if(element.isJsonPrimitive()) {

			JsonPrimitive p = element.getAsJsonPrimitive();

			if(p.isString()) {

				String replaced = p.getAsString().replace(url, "<base>");
				return new JsonPrimitive(replaced);
			}
		} 

		return element.deepCopy();
	}

	public JsonElement deepSort(JsonElement element) {

		if(element.isJsonArray()) {

			JsonArray arr = element.getAsJsonArray();

			JsonElement[] elems = new JsonElement[arr.size()];

			for(int i = 0; i < arr.size(); ++ i) {
				elems[i] = deepSort(arr.get(i));
			}
			
			Arrays.sort(elems, new Comparator<JsonElement>() {

				public int compare(JsonElement a, JsonElement b) {
					return gson.toJson(a).compareTo(gson.toJson(b));
				}
			});

			JsonArray res = new JsonArray();

			for(int i = 0; i < arr.size(); ++ i) {
				res.add(elems[i]);
			}

			return res;

		} else if(element.isJsonObject()) {

			JsonObject obj = element.getAsJsonObject();

			TreeSet<String> sortedKeys = new TreeSet<String>();

			for(String key : obj.keySet()) {
				sortedKeys.add(key);
			}

			JsonObject res = new JsonObject();

			for(String key : sortedKeys) {
				res.add(key, deepSort(obj.get(key)));
			}

			return res;

		}

		return element.deepCopy();
	}

	public JsonElement removeDates(JsonElement element) {

		if(element.isJsonArray()) {

			JsonArray arr = element.getAsJsonArray();
			JsonArray res = new JsonArray();
			
			for(int i = 0; i < arr.size(); ++ i) {
				res.add(removeDates(arr.get(i)));
			}

			return res;

		} else if(element.isJsonObject()) {

			JsonObject obj = element.getAsJsonObject();
			JsonObject res = new JsonObject();

			for(Entry<String, JsonElement> entry : obj.entrySet()) {

				if(entry.getKey().equals("loaded")) {
					res.add(entry.getKey(), new JsonPrimitive("<loaded>"));
					continue;
				}

				if(entry.getKey().equals("updated")) {
					res.add(entry.getKey(), new JsonPrimitive("<updated>"));
					continue;
				}

				res.add(entry.getKey(), removeDates(entry.getValue()));
			}

			return res;

		}

		return element.deepCopy();
	}

	/*
	public String removeBaseUrl(String url, String baseUrl) {

		if(!url.startsWith(baseUrl)) {
			throw new RuntimeException("url does not start with base url");
		}

		return url.substring(url.length());
	}*/

	public String doubleEncode(String iri) throws UnsupportedEncodingException {

		return URLEncoder.encode(URLEncoder.encode(iri, "utf-8"), "utf-8");
	}

	public String sanitizeFilename(String filename) {
		return filename.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
	}

}