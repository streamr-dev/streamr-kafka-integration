# Streamr Kafka Connect - Kafka Streams Integrations

This repository shows you how to connect your Streamr data to an Apache Kafka cluster with Streamr's Java client, and how to then process the data with Kafka Streams.

# Setting up
## The repository

1. Make sure that you have Java 8+ installed.
2. Clone the repo and run `mvn package` in the StreamrKafkaConnect project for Kafka Connect integration, and also in the kafka-streams project if you wish to use Kafka Streams to process your Streamr data.
3. Information on how to run the .jar files can be found below.

## Streamr
If you already have a running stream set up in Streamr and know how to set up a new stream for data flowing out of kafka you can skip this section.

1. Go to www.streamr.com, set up an account and log in
2. Log in and go to Streamr marketplace
3. Search for "Helsinki Trams GPS" and select the free marketplace stream that pops up
4. Add the stream to your purchases (again it is free) and then hover your mouse over the "Public transport demo" and click "Add to editor"
5. Go to Streamr core and select the Streams section. You should see the Public transport demo stream in the listing. You can click it to see the data flowing.
6. Next click the "create stream" button give it a name such as "kafka sink" and click save & exit.

Now when you see a reference to a Streamr stream ID it is the Stream ID that you can copy and paste in the details section of a stream.

You will also need your Streamr API key which you can find by going to the "API credentials" section in the editor.

## Kafka
If you are unfamiliar with [Apache Kafka](https://kafka.apache.org/) go to the quickstart section in Kafka's website and run through it. It's also a good idea to read through the documentation to understand the core concepts of Kafka. Setting up a kafka cluster locally for this repository is also explained below.

1. Go to the directory where you installed Kafka
2. Start a Zookeeper service with
```
bin/zookeeper-server-start.sh config/zookeeper.properties
```
3. Set up 3+ kafka servers running in the cluster.

    * First you need to set up .properties settings for each server.
    * In the config directory you should find a server.properties file. Run this command for server-0, server-1 and server-2.
    ```
    cp server.properties server-{index}.properties
    ```
    * Now go through each of the .properties and go through these fields:
        * listeners, add a free port in localhost or a remote machine (ie. localhost:9093, localhost:9094,..)
        * broker.id, add unique keys (0,1,2,..)
        * log.dirs, add a path to a directory for logs, for example a `/tmp` directory in the Kafka directory's root.
        * zookeeper.connect, make sure that this field is pointed to your zookeeper service's port.
        * In practice:
        ```
        broker.id=1
        listeners=PLAINTEXT://:9093
        log.dirs=/tmp/kafka-logs-1
        ```
    * Now set up a terminal for each server and run the command for each server-{index}.properties file
    ```
    bin/kafka-server-start.sh config/server.properties
    ```

4. Open up a new terminal and go to the kafka directory's root again and open topics for Streamr data with the commands:
    ```
    bin/kafka-topics.sh --create --bootstrap-server localhost:9093 --replication-factor 3 --partitions 1 --topic streamr-in

    ```
    ```
    bin/kafka-topics.sh --create --bootstrap-server localhost:9093 --replication-factor 3 --partitions 1 --topic streamr-out
    ```
    These topics will now run in the kafka cluster you set up earlier. You can run:
    ```
    bin/kafka-topics.sh --list --bootstrap-server localhost:9093
    ```
    and see if `streamr-in` and `streamr-out` are logged.
    Checkout https://kafka.apache.org/quickstart#quickstart_multibroker to find out how to see if data is flowing through the topics.

## Kafka Connect
After you have set up a kafka cluster with replicated topics and you have packaged the .jars it's time to set up Kafka Connect in distributed mode.
1. Run 
    ```
    cp config/connect-distributed.properties config/connect-streamr.properties
    ```
2. Open up the new connect-streamr.properties file and make these changes:
    * bootstrap.servers, add ports of the kafka servers in the cluster that you want to use here (ie. `bootstrap.servers=localhost:9093,localhost:9094,localhost:9095`). You can set up new servers to the cluster for Kafka Connect if you wish.
    * group.id, make sure that this name is unique.
    * rest.port, make sure that the port used in this field is free. (ie. localhost:8083)
    * plugin.path, this is where you put the path of this repository's StreamrKafkaConnect project's .jar file. The jar file should be in `streamr-kafka-integration/StreamrKafkaConnect/target/StreamrKafkaConnect-1.0-SNAPSHOT.jar` after you have packaged the project.
3. Run:
    ```
    bin/connect-distributed.sh config/connect-streamr.properties
    ```
    Now you are able to make rest calls to the rest API in the port that you specified in the `connect-streamr.properties` file.
4. Now you can make rest API calls to Kafka Connect to start up the Streamr integrations. The rest API calls can be found in the `curl-posts.txt` file in the repository. 
    * You should add your Streamr API key and your stream IDs to the curl scripts before running them. Add the stream id of the stream that you wish to subscribe to the Streamr subscribe curl script, and the stream ID of an empty stream to the Streamr publish curl script.
    * Also make sure that the curls are sent to the right address. (ie. `localhost:8083/connectors`)
    * If you want to try out if the data is flowing from Streamr to Kafka and back you can also set the Streamr publish curl script's kafka topic fields as `streamr-in`. However if you wish to move forwards to try out Kafka Streams you should keep the field as `streamr-out` or change it back. You also need to send a delete post request for the streamr-sink to restart it.
    * After the post requests are sent the data of the stream you are subscribing to should be flowing to the stream you are publishing to through Kafka. You can check this in Streamr's editor.

The Kafka Connect integration pushes the data from Streamr to Kafka as a JSON string. IF you wish to use another format or a data structure such as hashmap you can implement those in the source code's StreamrSourceConnectorTask and StreamrSinkConnectorTask classes.

## Kafka Streams
[Kafka Streams](https://kafka.apache.org/documentation/streams/) can be used to process data in Kafka. Streams does not process the data in the Kafka cluster itself, instead it processes the data in a seperate JVM. This allows you to use Kafka data in client side applications, or to do heavier computation on the data in server side. Importantly when compared to Data processing tools such as Apache Spark and Flink, Kafka Streams does not require a seperate cluster to run in. This means that you can use Kafka Streams in any of your existing Java or Scala applications.

To launch the example Kafka Streams project in the repository simply run 

```
mvn package
```

and then

```
mvn exec:java -Dexec.mainClass=myapps.Pepe
```

The example takes the data from the `streamr-in` topic and filters the trams that contain the substring `"6T"`. Then the filtered data is produced to the `streamr-out` topic in Kafka from which it is published to Streamr via Kafka Connect.