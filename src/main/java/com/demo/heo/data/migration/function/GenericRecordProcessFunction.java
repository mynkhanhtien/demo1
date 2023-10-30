package com.demo.heo.data.migration.function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

public class GenericRecordProcessFunction extends ProcessFunction<Row, GenericRecord> {

    private final String stringSchema;

    public GenericRecordProcessFunction(String stringSchema) {
        this.stringSchema = stringSchema;
    }

    @Override
    public void processElement(Row value, ProcessFunction<Row, GenericRecord>.Context ctx, Collector<GenericRecord> out) throws Exception {
        Schema schema = new Schema.Parser().parse(stringSchema);

        GenericRecord avroRecord = new GenericData.Record(schema);

        if (value.getArity() != schema.getFields().size()) {
            throw new IllegalArgumentException("Row and Avro schema field count must match.");
        }

        for (int i = 0; i < value.getArity(); i++) {
            // Get the Avro field name (assuming the order of fields matches)
            String fieldName = schema.getFields().get(i).name();

            // Get the value from the Row
            Object rowValue = value.getField(i);

            // Set the Avro field with the value from the Row
            avroRecord.put(fieldName, rowValue);
        }

        out.collect(avroRecord);
    }
}
