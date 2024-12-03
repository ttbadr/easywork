package com.example.dataflow;

import com.example.dataflow.config.TableMapping;
import com.example.dataflow.db.AlloyDBWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.ParDo;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PubSubToAlloyDB {

    /**
     * Options interface for the pipeline.
     */
    public interface Options extends DataflowPipelineOptions {
        @Description("List of Pub/Sub subscription names to read from, comma-separated")
        @Required
        String getSubscriptions();
        void setSubscriptions(String value);

        @Description("AlloyDB JDBC URL")
        @Required
        String getJdbcUrl();
        void setJdbcUrl(String value);

        @Description("Path to the table mapping configuration file")
        @Default.String("")
        String getTableMappingConfig();
        void setTableMappingConfig(String value);
    }

    public static void main(String[] args) throws Exception {
        Options options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(Options.class);

        // Load table mappings from configuration file
        ObjectMapper mapper = new YAMLMapper();
        List<TableMapping> tableMappings;
        
        String configPath = options.getTableMappingConfig();
        if (configPath != null && !configPath.isEmpty()) {
            // Load from specified file
            tableMappings = Arrays.asList(
                mapper.readValue(new File(configPath), TableMapping[].class));
        } else {
            // Load from default configuration in resources
            tableMappings = Arrays.asList(
                mapper.readValue(
                    PubSubToAlloyDB.class.getResourceAsStream("/table-mapping.yaml"),
                    TableMapping[].class));
        }

        Pipeline pipeline = Pipeline.create(options);

        // Process each subscription
        String[] subscriptions = options.getSubscriptions().split(",");
        for (String subscription : subscriptions) {
            pipeline
                .apply("Read From " + subscription,
                    PubsubIO.readStrings()
                        .fromSubscription(subscription))
                .apply("Write To AlloyDB " + subscription,
                    ParDo.of(new AlloyDBWriter(options.getJdbcUrl(), tableMappings)));
        }

        pipeline.run();
    }
}
