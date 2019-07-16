package ConnectorTasks;

import com.streamr.client.MessageHandler;
import com.streamr.client.StreamrClient;
import com.streamr.client.Subscription;
import com.streamr.client.authentication.ApiKeyAuthenticationMethod;
import com.streamr.client.options.StreamrClientOptions;
import com.streamr.client.protocol.message_layer.StreamMessage;
import com.streamr.client.rest.Stream;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class StreamrSourceConnectorTask extends SourceTask {
    private String kafkaTopic;
    private String streamrApiKey;
    private String streamrStreamId;
    private StreamrClient client;
    private Stream stream;
    private Subscription sub;
    private LinkedBlockingQueue<StreamMessage> queue = new LinkedBlockingQueue<>();

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
        sub = client.subscribe(stream, new MessageHandler() {
            @Override
            public void onMessage(Subscription subscription, StreamMessage streamMessage) {
                try {
                    queue.put(streamMessage);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        if (client.getState() == StreamrClient.State.Disconnected) {
            client.connect();
        }
        List<SourceRecord> records = new ArrayList<>();
        StreamMessage message = queue.take();
//        System.out.println(message.getSerializedContent());
        String json = message.getSerializedContent();
        SourceRecord record = new SourceRecord(null, null, kafkaTopic, Schema.STRING_SCHEMA, json);
        System.out.println(record);
        records.add(record);

        return records;
    }

    @Override
    public synchronized void stop() {
        client.unsubscribe(sub);
        client.disconnect();
    }

    @Override
    public String version() {
        return "0.0.1";
    }

}
