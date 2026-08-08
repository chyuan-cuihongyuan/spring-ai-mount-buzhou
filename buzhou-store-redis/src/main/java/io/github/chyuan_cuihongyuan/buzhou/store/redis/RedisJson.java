package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * Redis 序列化助手（对标 {@code JdbcJson}）：记录 ↔ JSON String。
 *
 * <p>注册 {@link JavaTimeModule} 以保真 {@code Instant}（startedAt/occurredAt/createdAt 等）。
 */
final class RedisJson {

    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private RedisJson() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    static <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("JSON deserialize failed", e);
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
}
