package uk.ac.ebi.spot.ols.config;

import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class MatomoTrackingBuffer {

    private static final Logger log = LoggerFactory.getLogger(MatomoTrackingBuffer.class);
    private static final int FLUSH_THRESHOLD = 20; // flush early if batch grows large

    private final MatomoTracker tracker;
    private final ConcurrentLinkedQueue<MatomoRequest> queue = new ConcurrentLinkedQueue<>();

    public MatomoTrackingBuffer(MatomoTracker tracker) {
        this.tracker = tracker;
    }

    public void enqueue(MatomoRequest request) {
        queue.add(request);
        if (queue.size() >= FLUSH_THRESHOLD) {
            flush();
        }
    }

    @Scheduled(fixedDelay = 5000) // flush every 5 seconds
    public void flush() {
        if (queue.isEmpty()) return;

        List<MatomoRequest> batch = new ArrayList<>();
        MatomoRequest request;
        while ((request = queue.poll()) != null) {
            batch.add(request);
        }

        if (batch.isEmpty()) return;

        log.debug("Flushing {} Matomo tracking requests", batch.size());

        tracker.sendBulkRequestAsync(batch.toArray(new MatomoRequest[0]))
                .exceptionally(ex -> {
                    log.error("Matomo bulk tracking failed for {} requests: {}", batch.size(), ex.getMessage());
                    return null;
                });
    }
}
