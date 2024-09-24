package uk.ac.ebi.rdf2json;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;

public class Worker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Worker.class);

	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private String ontologyId;
	private boolean loadLocalFiles;
	private String downloadedPath;
	private Map<String, Object> config;
	private boolean noDates;
	private boolean convertToRDF;
	private JsonWriterWrapper writerWrapper;

	public Worker(Map<String, Object> config, boolean loadLocalFiles, boolean noDates, String downloadedPath,
			boolean convertToRDF, String ontologyId) throws IOException {
		this.loadLocalFiles = loadLocalFiles;
		this.downloadedPath = downloadedPath;
		this.config = config;
		this.noDates = noDates;
		this.convertToRDF = convertToRDF;
		this.writerWrapper = JsonWriterWrapper.getInstance(downloadedPath);
		this.ontologyId = ontologyId;
	}

	public JsonWriterWrapper getWriterWrapper() {
		return writerWrapper;
	}

	public void setWriterWrapper(JsonWriterWrapper writerWrapper) {
		this.writerWrapper = writerWrapper;
	}

	@Override
	public void run() {
		try {
			logger.info("Ontology parsing by thread: {}", Thread.currentThread().getName());
			//OntologyGraph graph = new OntologyGraph(config, loadLocalFiles, noDates, downloadedPath, convertToRDF);

			
			// lock.writeLock().lock();
			synchronized (lock) {
				OntologyGraph graph = new OntologyGraph(config, loadLocalFiles, noDates, downloadedPath, convertToRDF);
				if (graph.ontologyNode == null) {
					logger.error("No Ontology node found; nothing will be written");
				}
				System.out.println("Thread acquired lock: "+ Thread.currentThread().getName());
				JsonWriter writer = writerWrapper.getWriter();
				System.out.println(writer.toString());
				graph.write(writer);
				writerWrapper.setWriter(writer);
			}

			/*
			 * }finally { lock.writeLock().unlock(); }
			 */
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
