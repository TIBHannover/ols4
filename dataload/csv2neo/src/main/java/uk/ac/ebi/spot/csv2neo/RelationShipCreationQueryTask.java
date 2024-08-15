package uk.ac.ebi.spot.csv2neo;

import org.apache.commons.csv.CSVRecord;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static uk.ac.ebi.spot.csv2neo.QueryGeneration.*;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
public class RelationShipCreationQueryTask implements Runnable {

    private final Driver driver;
    private final CountDownLatch latch;
    private final List<CSVRecord> records;
    private final String[] headers;
    private final File file;
    private final int attempts;

    public RelationShipCreationQueryTask(Driver driver, CountDownLatch latch, List<CSVRecord> records, String[] headers, File file, int attempts) {
        this.driver = driver;
        this.latch = latch;
        this.records = records;
        this.headers = headers;
        this.file = file;
        this.attempts = attempts;
    }

    @Override
    public void run() {
        boolean success = false;
        for(int i = 0;i<attempts;i++){
            try (Session session = driver.session()) {
                if(!success){
                    success = session.writeTransaction(tx -> {
                        for (CSVRecord csvRecord : records) {
                            String[] row = csvRecord.toList().toArray(String[]::new);
                            String query = generateRelationCreationQuery(headers,row);
                            tx.run(query);
                        }
                        return true;
                    });
                    latch.countDown();
                }
            } catch(Exception e){
                System.out.println("Attempt "+i+" error: "+e.getMessage());
            }
        }
    }
}
