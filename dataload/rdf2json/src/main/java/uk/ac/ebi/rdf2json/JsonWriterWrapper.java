package uk.ac.ebi.rdf2json;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.stream.JsonWriter;

public class JsonWriterWrapper {
	private static JsonWriterWrapper single_instance = null;
	private JsonWriter writer;
	private ReentrantLock lock = new ReentrantLock();
	
	private JsonWriterWrapper(String outputFilePath) throws IOException {
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
	
	public void initWriter() throws IOException {
		writer.setIndent("  ");

        writer.beginObject();

        writer.name("ontologies");
        
        writer.beginArray();
	}
	
	public void endWriter() throws IOException {
		writer.endArray();
		
        writer.endObject();

        writer.close();
	}
	
	public void graphWriter(OntologyGraph graph) throws Throwable {
		lock.lock();
		try {
			graph.write(writer);
		}finally {
			lock.unlock();
		}
	}
	
	
}
