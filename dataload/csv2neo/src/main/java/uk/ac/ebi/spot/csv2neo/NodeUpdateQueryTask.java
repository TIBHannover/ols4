package uk.ac.ebi.spot.csv2neo;

import org.apache.commons.csv.CSVRecord;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static uk.ac.ebi.spot.csv2neo.QueryGeneration.generateUpdateQuery;
import static uk.ac.ebi.spot.csv2neo.QueryGeneration.generatePropsForUpdate;

/**
 * @author Erhun Giray TUNCAY
 * @email giray.tuncay@tib.eu
 * TIB-Leibniz Information Center for Science and Technology
 */
public class NodeUpdateQueryTask implements Runnable {

    private final Driver driver;
    private CountDownLatch latch;
    private final List<CSVRecord> records;
    private final String[] headers;
    private final File file;
    private final int attempts;
    private final List<String> subset;

    public NodeUpdateQueryTask(Driver driver, CountDownLatch latch, List<CSVRecord> records, String[] headers, List<String> subset, File file, int attempts) {
        this.driver = driver;
        this.latch = latch;
        this.records = records;
        this.headers = headers;
        this.file = file;
        this.attempts = attempts;
        this.subset = subset;
    }

    @Override
    public void run() {
        boolean success = false;
        for(int i = 0;i<attempts;i++){
            try (Session session = driver.session()) {
                if(!success){
                    success =session.executeWrite(tx -> {
                        for (CSVRecord csvRecord : records) {
                            String[] row = csvRecord.toList().toArray(String[]::new);
                            String query = generateUpdateQuery(headers, row);
                            Map<String, Object> params = generatePropsForUpdate(headers, row, subset);
                            tx.run(query, params);
                        }
                        return true;
                    });
                }
            } catch(Exception e) {
                System.out.println("Attempt "+i+" error: "+e.getMessage());
            }
        }
        latch.countDown();
        System.out.println("There are "+latch.getCount()+" remaining node batches.");
        if (success)
            System.out.println(records.size()+" nodes has been successfully updated from "+file.getName());
        else
            System.out.println("Warning: "+records.size()+" nodes failed to be updated from "+file.getName());
    }
}
