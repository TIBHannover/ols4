package uk.ac.ebi.spot.csv2neo;

import org.apache.commons.csv.CSVRecord;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static uk.ac.ebi.spot.csv2neo.QueryGeneration.generateBlankNodeCreationQuery;
import static uk.ac.ebi.spot.csv2neo.QueryGeneration.generateProps;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
public class NodeCreationQueryTask implements Runnable {

    private final Driver driver;
    private final CountDownLatch latch;
    private final List<CSVRecord> records;
    private final String[] headers;
    private final File file;

    public NodeCreationQueryTask(Driver driver,CountDownLatch latch, List<CSVRecord> records, String[] headers, File file) {
        this.driver = driver;
        this.latch = latch;
        this.records = records;
        this.headers = headers;
        this.file = file;

    }

    @Override
    public void run() {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                for (CSVRecord csvRecord : records) {
                    String[] row = csvRecord.toList().toArray(String[]::new);
                    String query = generateBlankNodeCreationQuery(headers,row);
                    Map<String,Object> params = generateProps(headers,row);
                    if(query.isEmpty())
                        System.out.println("empty query for appended line: "+ Arrays.toString(row)+" in file: "+file);
                    else
                        tx.run(query,params);
                }
                return null;
            });
        } finally {
            latch.countDown();
        }
    }
}
