package uk.ac.ebi.rdf2json;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;

public class Worker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Worker.class);

	private ReentrantLock lock = new ReentrantLock();

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

	@Override
	public void run() {
		try {
			OntologyGraph graph = new OntologyGraph(config, loadLocalFiles, noDates, downloadedPath, convertToRDF);
			System.out.println("graph is ready");
			if (graph.ontologyNode == null) {
				logger.error("No Ontology node found; nothing will be written");
			}else {
				lock.lock();
				try {
					logger.info("Ontology {} parsing by thread: {}",ontologyId, Thread.currentThread().getName());
					writerWrapper.graphWriter(graph);
				} finally {
					logger.info("Ontology {} is written by thread: {}",ontologyId, Thread.currentThread().getName());
					lock.unlock();
				}
			}

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
