package com.demo.heo.data.migration;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Slf4j
public class DemoJob {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        DataStream<Tuple2<String, Integer>> dataStream = env.fromElements(
                new Tuple2<>("A", 1),
                new Tuple2<>("B", 2),
                new Tuple2<>("C", 3)
        ).setParallelism(1);

        DataStream<String> resultStream = dataStream.map((MapFunction<Tuple2<String, Integer>, String>) value -> {
            // Log a message
            log.info("Processing: " + value.f0);
            return value.f0;
        });

        resultStream.print(); // This will print the processed data to stdout

        env.execute("Flink Job with Logging ");
    }
}
