package com.demo.heo.data.migration.utils;

import org.apache.avro.Schema;

import java.util.List;

public class ConvertUtils {
    public static String generateCreateTableSQL(Schema schema, String table) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("CREATE TABLE %s (", table));

        List<Schema.Field> fields = schema.getFields();
        for (int i = 0; i < fields.size(); i++) {
            Schema.Field field = fields.get(i);
            String fieldName = field.name();
            String fieldType = field.schema().getType().getName().toLowerCase();

            sb.append("\n  ").append(fieldName).append(" ").append(fieldType);
            if (i < fields.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("\n) WITH (");
        sb.append("\n  'connector' = 'jdbc',");
        sb.append("\n  'url' = 'jdbc:postgresql://localhost:5432/sourceDB',");
        sb.append("\n  'table-name' = 'member',");
        sb.append("\n  'username' = 'postgres',");
        sb.append("\n  'password' = '12345678x@X'");
        sb.append("\n)");

        return sb.toString();
    }
}
