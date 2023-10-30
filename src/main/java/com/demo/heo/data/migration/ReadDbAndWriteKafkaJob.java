package com.demo.heo.data.migration;

import com.amazonaws.services.schemaregistry.flink.avro.GlueSchemaRegistryAvroSerializationSchema;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.demo.heo.data.migration.function.GenericRecordProcessFunction;
import com.demo.heo.data.migration.records.GenericRecordSerializer;
import com.demo.heo.data.migration.utils.ConvertUtils;
import com.twitter.chill.java.UnmodifiableCollectionSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import software.amazon.awssdk.services.glue.model.DataFormat;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ReadDbAndWriteKafkaJob {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // Define schema and source table
        Class<?> unmodColl = Class.forName("java.util.Collections$UnmodifiableCollection");
        env.getConfig().addDefaultKryoSerializer(unmodColl, UnmodifiableCollectionSerializer.class);

        String memberSchema = Files.readString(Paths.get(
                Objects.requireNonNull(
                        ReadDbAndWriteKafkaJob.class.getClassLoader()
                                .getResource("member.avsc")).toURI()));
        Schema schema = new Schema.Parser().parse(memberSchema);
        String sourceTableName = "source_" + schema.getName().toLowerCase();
        tEnv.executeSql(ConvertUtils.generateCreateTableSQL(schema, sourceTableName));

        // Define kafka Sink
        String topic = schema.getName().toLowerCase();
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9094");
        properties.setProperty("group.id", "test");

        Map<String, Object> configs = new HashMap<>();
        configs.put(AWSSchemaRegistryConstants.AWS_REGION, "us-east-1");
        configs.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, true);

        FlinkKafkaProducer<GenericRecord> producer =  new FlinkKafkaProducer<>(
                topic,
                GlueSchemaRegistryAvroSerializationSchema.forGeneric(schema, topic, configs),
                properties);

        // Execute job
        Table sourceTable = tEnv.from(sourceTableName);
        tEnv.toDataStream(sourceTable).process(new GenericRecordProcessFunction(memberSchema)).print();
        env.execute();
    }
}
