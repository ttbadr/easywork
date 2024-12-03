package com.example.dataflow.metrics;

import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;

/**
 * Metrics collector for AlloyDB operations.
 */
public class AlloyDBMetrics {
    private final Counter successfulInserts;
    private final Counter failedInserts;
    private final Counter retries;
    private final Counter finalFailures;
    private final Distribution insertLatency;
    private final Distribution batchSize;
    
    public AlloyDBMetrics(String name) {
        String namespace = "alloydb_metrics";
        this.successfulInserts = Metrics.counter(namespace, name + "_successful_inserts");
        this.failedInserts = Metrics.counter(namespace, name + "_failed_inserts");
        this.retries = Metrics.counter(namespace, name + "_retries");
        this.insertLatency = Metrics.distribution(namespace, name + "_insert_latency_ms");
        this.batchSize = Metrics.distribution(namespace, name + "_batch_size");
        this.finalFailures = Metrics.counter(namespace, name + "_final_failures");
    }
    
    public void recordSuccessfulInsert() {
        successfulInserts.inc();
    }
    
    public void incrementSuccessfulInserts(int count) {
        successfulInserts.inc(count);
    }
    
    public void recordFailedInsert() {
        failedInserts.inc();
    }
    
    public void incrementFailedInserts(int count) {
        failedInserts.inc(count);
    }
    
    public void recordRetry() {
        retries.inc();
    }
    
    public void recordLatency(long startTimeMillis) {
        insertLatency.update(System.currentTimeMillis() - startTimeMillis);
    }
    
    public void recordBatchSize(int size) {
        batchSize.update(size);
    }
    
    public void incrementFinalFailures() {
        finalFailures.inc();
    }
    
    public void recordFinalFailure() {
        finalFailures.inc();
    }
}
