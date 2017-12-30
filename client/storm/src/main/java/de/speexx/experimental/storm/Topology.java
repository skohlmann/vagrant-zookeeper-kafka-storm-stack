package de.speexx.experimental.storm;

import java.io.IOException;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sascha.kohlmann
 */
public class Topology {

    private static final Logger LOG = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        final TopologyBuilder tb = new TopologyBuilder();
        tb.setSpout("kafka_spout", new KafkaSpout<>(KafkaSpoutConfig.builder(kafkaServer() + ":" + kafkaPort(), "topic").build()), 1);

        final BaseBasicBolt bolt = new BaseBasicBolt() {
            private static final long serialVersionUID = -4068637326848549242L;
            public void execute(final Tuple tuple, final BasicOutputCollector collector) {
                final int tupleSize = tuple.size();
                for (int i = 0; i < tupleSize; i++) {
                    final String msg = tuple.getString(i);
                    LOG.info("Message: " + msg);
                }
            }

            @Override
            public void declareOutputFields(final OutputFieldsDeclarer ofd) {
                return;
            }
        };
        
        tb.setBolt("print", bolt).shuffleGrouping("kafka_spout");
        final Config conf = new Config();
        conf.setDebug(true);
        conf.setNumWorkers(1);
        StormSubmitter.submitTopology("perfPrint", conf, tb.createTopology());

    }
    
    static Properties kafkaConsumerConfiguration() throws IOException {
        
        final Properties properties = fetchKafkaProperties();
        
        final Properties kafkaProperties = new Properties();
        kafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getProperty("kafka.server") + ":" + properties.getProperty("kafka.port"));
        kafkaProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return kafkaProperties;
    }

    static String kafkaTopicName() throws IOException {
        return fetchKafkaProperties().getProperty("kafka.topic");
    }
    
    static String kafkaServer() throws IOException {
        return fetchKafkaProperties().getProperty("kafka.server");
    }
    
    static String kafkaPort() throws IOException {
        return fetchKafkaProperties().getProperty("kafka.port");
    }
    
    static Properties fetchKafkaProperties() throws IOException {
        final Properties properties = new Properties();
        properties.load(Topology.class.getResourceAsStream("/META-INF/kafka.properties"));
        return properties;
    }
}
