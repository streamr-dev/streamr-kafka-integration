package ConnectorTasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamr.client.StreamrClient;
import com.streamr.client.authentication.ApiKeyAuthenticationMethod;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.rest.Stream;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class StreamrSinkConnectorTask extends SinkTask {

    private String kafkaTopic;
    private String streamrApiKey;
    private String streamrStreamId;
    private StreamrClient client;
    private Stream stream;

    public void start(Map<String, String> props) {
        streamrApiKey = props.get("STREAMR_API_KEY");
        streamrStreamId = props.get("STREAMR_STREAM_ID");
        kafkaTopic = props.get("KAFKA_TOPIC");

        client = new StreamrClient(new StreamrClientOptions(new ApiKeyAuthenticationMethod(streamrApiKey)));
        try {
            stream = client.getStream(streamrStreamId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void put(Collection<SinkRecord> records) {
        if (client.getState() == StreamrClient.State.Disconnected) {
            client.connect();
        }
//        System.out.println(records);
        for (SinkRecord rec : records) {
            String json = new String((String) rec.value()); // Transfer the bytes in to a JSON string
//            System.out.println(json);
            System.out.println(client.getState());
            System.out.println(stream.getName());
            final ObjectMapper mapper = new ObjectMapper();
            try {
                // If this fails the JSON string isn't valid and a transfer to the FAILURE relationship is done instead.
                LinkedHashMap<String, Object> msg = mapper.readValue(json, LinkedHashMap.class); //convert the JSON string to Streamr's Java client format
                System.out.println(msg.toString());
                client.publish(stream, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void stop() {
        client.disconnect();
    }
    public String version() {
        return "0.0.1";
    }
}
