import ConnectorTasks.StreamrSourceConnectorTask;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.connect.source.SourceTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamrSourceConnector extends SourceConnector {
    private String kafkaTopic;
    private String streamrApiKey;
    private String streamrStreamId;
    private int partition;

    @Override
    public Class<? extends Task> taskClass() {
        return StreamrSourceConnectorTask.class;
    }

    @Override
    public void start(Map<String, String> props) {

        streamrApiKey = props.get("STREAMR_API_KEY");
        streamrStreamId = props.get("STREAMR_STREAM_ID");
        kafkaTopic = props.get("KAFKA_TOPIC");
    }

    @Override
    public void stop() {
    }
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        ArrayList<Map<String, String>> configs = new ArrayList<>();
        // Only one input partition makes sense.

        Map<String, String> config = new HashMap<>();
        if (streamrApiKey != null) {
            config.put("STREAMR_API_KEY", streamrApiKey);
        }
        if (streamrStreamId != null) {
            config.put("STREAMR_STREAM_ID", streamrStreamId);
        }
        config.put("KAFKA_TOPIC", kafkaTopic);
        configs.add(config);
        return configs;
    }
    @Override
    public String version() {
        return "0.0.1";
    }
    @Override
    public ConfigDef config() {
        ConfigDef def = new ConfigDef();
        return def;
    }
}

