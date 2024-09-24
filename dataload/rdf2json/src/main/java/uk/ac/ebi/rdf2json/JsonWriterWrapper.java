package uk.ac.ebi.rdf2json;

import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.stream.JsonWriter;

public class JsonWriterWrapper {
	private static JsonWriterWrapper single_instance = null;
	private JsonWriter writer;
	private String outputFilePath;
	
	private JsonWriterWrapper(String outputFilePath) throws IOException {
		this.outputFilePath = outputFilePath;
		this.writer = new JsonWriter(new FileWriter(outputFilePath));
	}

	public static synchronized JsonWriterWrapper getInstance(String outputFilePath) throws IOException
    {
        if (single_instance == null)
        	single_instance = new JsonWriterWrapper(outputFilePath);
 
        return single_instance;
    }

	public JsonWriter getWriter() {
		return writer;
	}

	public void setWriter(JsonWriter writer) {
		this.writer = writer;
	}
	
	public void initWriter() throws IOException {
		writer = getWriter();
		writer.setIndent("  ");

        writer.beginObject();

        writer.name("ontologies");
        writer.beginArray();
        setWriter(writer);
	}
	
	public void endWriter() throws IOException {
		writer = getWriter();
		writer.endArray();
        writer.endObject();

        writer.close();
        setWriter(writer);
	}
	
	
}
