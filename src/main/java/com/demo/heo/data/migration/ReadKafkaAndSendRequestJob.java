package com.demo.heo.data.migration;

import com.amazonaws.services.schemaregistry.flink.avro.GlueSchemaRegistryAvroDeserializationSchema;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.amazonaws.services.schemaregistry.utils.AvroRecordType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ReadKafkaAndSendRequestJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Define kafka source
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9094");
        properties.setProperty("group.id", "test");

        Map<String, Object> configs = new HashMap<>();
        configs.put(AWSSchemaRegistryConstants.AWS_REGION, "us-east-1");
        configs.put(AWSSchemaRegistryConstants.AVRO_RECORD_TYPE, AvroRecordType.GENERIC_RECORD.getName());

        String memberSchema = Files.readString(Paths.get(
                Objects.requireNonNull(
                        ReadDbAndWriteKafkaJob.class.getClassLoader()
                                .getResource("member.avsc")).toURI()));
        Schema schema = new Schema.Parser().parse(memberSchema);

        String topic = schema.getName().toLowerCase();

        FlinkKafkaConsumer<GenericRecord> consumer = new FlinkKafkaConsumer<>(
                topic,
                GlueSchemaRegistryAvroDeserializationSchema.forGeneric(schema, configs),
                properties);
        DataStream<GenericRecord> stream = env.addSource(consumer);

        // Execute job
        stream.print();
        env.execute();
    }
}
