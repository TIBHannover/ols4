
package uk.ac.ebi.ols4.predownloader;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;

public class BulkOntologyDownloader {

    static final int NUM_THREADS = 16;

    Set<String> urlsToDownload;
    Set<String> urlsAlreadyProcessed;
    String downloadPath;
    boolean loadLocalFiles;
    Map<String, String> fileNameForDownloadByPurl;

    Set<OntologyDownloaderThread> threads = new HashSet<>();

    public BulkOntologyDownloader(List<String> ontologyUrls, String downloadPath, boolean loadLocalFiles, Map<String, String> fileNameForDownloadByPurl) {
        this.urlsToDownload = new LinkedHashSet<String>(ontologyUrls);
        this.urlsAlreadyProcessed = new HashSet<>();
        this.downloadPath = downloadPath;
        this.loadLocalFiles = loadLocalFiles;
        this.fileNameForDownloadByPurl = fileNameForDownloadByPurl;
    }

    public void downloadAll() {

	while(urlsToDownload.size() > 0) {

		List<Thread> threads = new ArrayList<>();
		Set<String> imports = new LinkedHashSet<>();

		for(int i = 0; i < NUM_THREADS; ++ i) {

			if(urlsToDownload.size() == 0) {
				break;
			}

			Iterator<String> it = urlsToDownload.iterator();
			String nextUrl = it.next();
			it.remove();

			urlsAlreadyProcessed.add(nextUrl);

			OntologyDownloaderThread downloader =
				new OntologyDownloaderThread(this, nextUrl, importUrls -> {
					imports.addAll(importUrls);
				});

			Thread t = new Thread(downloader, "Downloader thread " + i);
			threads.add(t);

			t.start();
		}

		for(Thread t : threads) {
			try {
				t.join();
				System.out.println(t.getName() + " finished");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for(String importUrl : imports) {
			if(!urlsAlreadyProcessed.contains(importUrl)) {
				urlsAlreadyProcessed.add(importUrl);
				urlsToDownload.add(importUrl);
			}
		}
	}

    }

}
