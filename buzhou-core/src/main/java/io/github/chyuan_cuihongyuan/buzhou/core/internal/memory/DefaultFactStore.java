package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 默认 {@link FactStore} 实现：建在 {@link SessionStateStore} 上。
 *
 * <p>事实序列化为 JSON 存入 {@link StateEntry#value()}：{@code {"value":..., "producer":..., "createdTurn":..., "ttl":...}}。
 * key 命名空间 {@code fact.{producer}.{name}}；ttl 轮次过滤 {@code currentTurn - createdTurn < ttl}。
 */
public class DefaultFactStore implements FactStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FACT_KEY_PREFIX = "fact.";

    private final SessionStateStore stateStore;

    public DefaultFactStore(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @Override
    public void save(String sessionId, Fact fact) {
        String key = fact.key().startsWith(FACT_KEY_PREFIX) ? fact.key()
                : Fact.keyFor(fact.producer(), fact.key());
        Map<String, Object> envelope = new java.util.LinkedHashMap<>();
        envelope.put("value", fact.value());
        envelope.put("producer", fact.producer());
        envelope.put("createdTurn", fact.createdTurn());
        envelope.put("ttl", fact.ttl());
        try {
            String json = MAPPER.writeValueAsString(envelope);
            stateStore.put(sessionId, new StateEntry(key, json, fact.producer(),
                    fact.createdTurn(), fact.ttl(), Instant.now()));
        } catch (Exception e) {
            // 退化：存 String.valueOf
            stateStore.put(sessionId, new StateEntry(key, String.valueOf(fact.value()), fact.producer(),
                    fact.createdTurn(), fact.ttl(), Instant.now()));
        }
    }

    @Override
    public List<Fact> activeFacts(String sessionId, int currentTurn) {
        Map<String, StateEntry> all = stateStore.getAll(sessionId);
        List<Fact> facts = new ArrayList<>();
        for (StateEntry entry : all.values()) {
            if (entry.key() == null || !entry.key().startsWith(FACT_KEY_PREFIX)) {
                continue;
            }
            Fact fact = deserialize(entry);
            if (fact == null) {
                continue;
            }
            // ttl 过滤：currentTurn - createdTurn < ttl（createdTurn 当轮即注入）
            if (currentTurn - fact.createdTurn() < fact.ttl()) {
                facts.add(fact);
            }
        }
        facts.sort(Comparator.comparingInt(Fact::createdTurn));
        return facts;
    }

    @Override
    public void delete(String sessionId, String key) {
        stateStore.delete(sessionId, key);
    }

    @SuppressWarnings("unchecked")
    private Fact deserialize(StateEntry entry) {
        try {
            Map<String, Object> envelope = MAPPER.readValue(entry.value(), new TypeReference<>() {
            });
            Object value = envelope.get("value");
            String producer = envelope.get("producer") instanceof String s ? s : entry.producer();
            int createdTurn = envelope.get("createdTurn") instanceof Number n ? n.intValue() : entry.createdTurn();
            int ttl = envelope.get("ttl") instanceof Number n ? n.intValue()
                    : (entry.ttlTurns() == null ? 1 : entry.ttlTurns());
            return new Fact(entry.key(), value, producer, createdTurn, ttl);
        } catch (Exception e) {
            // 退化：把 value 当原始字符串
            int ttl = entry.ttlTurns() == null ? 1 : entry.ttlTurns();
            return new Fact(entry.key(), entry.value(), entry.producer(), entry.createdTurn(), ttl);
        }
    }
}
