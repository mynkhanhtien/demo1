package com.demo.heo.data.migration.records;

import com.amazonaws.services.schemaregistry.common.configs.GlueSchemaRegistryConfiguration;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistrySerializer;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistrySerializerImpl;
import com.amazonaws.services.schemaregistry.utils.AVROUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GenericRecordSerializer implements KafkaRecordSerializationSchema<GenericRecord> {

    private String topic;
    private String stringSchema;

    private static GlueSchemaRegistrySerializer glueSchemaRegistrySerializer;

    private static AwsCredentialsProvider awsCredentialsProvider =
            DefaultCredentialsProvider
                    .builder()
                    .build();

    public GenericRecordSerializer(String topic, String stringSchema) {

        this.stringSchema = stringSchema;
        this.topic = topic;

        //Glue Schema Registry serializer initialization for the producer.
        glueSchemaRegistrySerializer =
                new GlueSchemaRegistrySerializerImpl(
                        awsCredentialsProvider,
                        getSchemaRegistryConfiguration("us-east-1")
                );
    }

    @Override
    public void open(SerializationSchema.InitializationContext context, KafkaSinkContext sinkContext) throws Exception {
        KafkaRecordSerializationSchema.super.open(context, sinkContext);
    }

    @Nullable
    @Override
    public ProducerRecord<byte[], byte[]> serialize(GenericRecord element, KafkaSinkContext context, Long timestamp) {

        Schema schema = new Schema.Parser().parse(stringSchema);
        byte[] recordWithSchema = encodeRecord(element, "test", null);

        return new ProducerRecord<>(topic, recordWithSchema);
    }


    private static GlueSchemaRegistryConfiguration getSchemaRegistryConfiguration(String regionName) {
        GlueSchemaRegistryConfiguration configs = new GlueSchemaRegistryConfiguration(regionName);
        //Optional setting to enable auto-registration.
        configs.setSchemaAutoRegistrationEnabled(true);
        //Optional setting to define metadata for the schema version while auto-registering.
        configs.setMetadata(getMetadata());
        return configs;
    }

    private static byte[] encodeRecord(GenericRecord record, String streamName, com.amazonaws.services.schemaregistry.common.Schema gsrSchema) {
        byte[] recordAsBytes = convertRecordToBytes(record);
        //Pass the GSR Schema and record payload to glueSchemaRegistrySerializer.encode method.
        byte[] recordWithSchemaHeader =
                glueSchemaRegistrySerializer.encode(streamName, gsrSchema, recordAsBytes);
        return recordWithSchemaHeader;
    }

    private static byte[] convertRecordToBytes(final Object record) {
        //Standard Avro code to convert records into bytes.
        ByteArrayOutputStream recordAsBytes = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(recordAsBytes, null);
        GenericDatumWriter datumWriter = new GenericDatumWriter<>(AVROUtils.getInstance().getSchema(record));
        try {
            datumWriter.write(record, encoder);
            encoder.flush();
        } catch (IOException e) {
            log.warn("Failed to convert record to Bytes" + e.getMessage());
            throw new UncheckedIOException(e);
        }
        return recordAsBytes.toByteArray();
    }

    private static Map<String, String> getMetadata() {
        //Metadata is optionally used by GSR while auto-registering a new schema version.
        Map<String, String> metadata = new HashMap<>();
        metadata.put("event-source-1", "topic1");
        metadata.put("event-source-2", "topic2");
        metadata.put("event-source-3", "topic3");
        return metadata;
    }
}
