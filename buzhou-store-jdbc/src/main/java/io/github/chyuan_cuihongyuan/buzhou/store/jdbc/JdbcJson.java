package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

final class JdbcJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JdbcJson() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    static Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("JSON deserialize failed", e);
        }
    }

    static <T> java.util.List<T> readList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return java.util.List.of();
        }
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(java.util.List.class, elementType));
        } catch (Exception e) {
            throw new IllegalStateException("JSON deserialize failed", e);
        }
    }
}
