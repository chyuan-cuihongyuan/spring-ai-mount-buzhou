package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 消息静态加密装饰器（spec 333 / T657）：append 时每条消息序列化 +
 * {@link EnvelopeCipher} 加密成<b>载体消息</b>（id/sessionId/turnSeq/
 * seqInTurn/createdAt 明文供排序与检索路由；role 以 USER 占位、真值在
 * 密文内；content 字段承载信封）；load/findById 解密还原。
 *
 * <p><b>透传兼容</b>：底层非载体旧明文原样返回（既有库迁移友好——不炸
 * 不重复加密）；已是信封的输入不再包装（幂等安全）。
 * <b>AAD 绑定</b>：每条消息以 {@code id + "\n" + sessionId} 绑定——密文
 * 剪贴到别的消息/会话解密失败（完整性优先于可用性）。
 */
public final class EncryptingMessageStore implements MessageStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MessageStore delegate;
    private final EnvelopeCipher cipher;

    public EncryptingMessageStore(MessageStore delegate, EnvelopeCipher cipher) {
        this.delegate = java.util.Objects.requireNonNull(delegate);
        this.cipher = java.util.Objects.requireNonNull(cipher);
    }

    @Override
    public void append(String sessionId, List<BuzhouMessage> messages) {
        List<BuzhouMessage> carriers = new ArrayList<>(messages.size());
        for (BuzhouMessage message : messages) {
            carriers.add(toCarrier(message));
        }
        delegate.append(sessionId, carriers);
    }

    @Override
    public List<BuzhouMessage> load(String sessionId) {
        List<BuzhouMessage> stored = delegate.load(sessionId);
        List<BuzhouMessage> result = new ArrayList<>(stored.size());
        for (BuzhouMessage message : stored) {
            result.add(fromCarrier(message));
        }
        return result;
    }

    @Override
    public Optional<BuzhouMessage> findById(String messageId) {
        return delegate.findById(messageId).map(this::fromCarrier);
    }

    @Override
    public void deleteSession(String sessionId) {
        delegate.deleteSession(sessionId);
    }

    /** 被装饰的底层 store（观测/装配面）。 */
    public MessageStore delegate() {
        return delegate;
    }

    // ---- 载体往返 ----

    private BuzhouMessage toCarrier(BuzhouMessage message) {
        if (EnvelopeCipher.isEnvelope(message.content())) {
            return message; // 已是信封——不再包装（幂等安全）
        }
        String envelope = cipher.encrypt(serialize(message), aadOf(message));
        return new BuzhouMessage(message.id(), message.sessionId(), message.turnSeq(),
                message.seqInTurn(), Role.USER, envelope, List.of(), null, null, null,
                Map.of(), message.createdAt());
    }

    private BuzhouMessage fromCarrier(BuzhouMessage stored) {
        if (!EnvelopeCipher.isEnvelope(stored.content())) {
            return stored; // 旧明文透传（迁移友好）
        }
        return deserialize(cipher.decrypt(stored.content(), aadOf(stored)));
    }

    private static String aadOf(BuzhouMessage message) {
        return message.id() + "\n" + message.sessionId();
    }

    // ---- 归一序列化（不依赖 jackson-jsr310：Instant→epochMillis、枚举→name） ----

    private static String serialize(BuzhouMessage message) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("id", message.id());
        normalized.put("sessionId", message.sessionId());
        normalized.put("turnSeq", message.turnSeq());
        normalized.put("seqInTurn", message.seqInTurn());
        normalized.put("role", message.role() == null ? null : message.role().name());
        normalized.put("content", message.content());
        normalized.put("toolCalls", toolCallsToMaps(message.toolCalls()));
        normalized.put("toolCallId", message.toolCallId());
        normalized.put("reasoningContent", message.reasoningContent());
        normalized.put("reasoningSignature", message.reasoningSignature());
        normalized.put("metadata", message.metadata());
        normalized.put("createdAt", message.createdAt() == null
                ? null : message.createdAt().toEpochMilli());
        try {
            return MAPPER.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new IllegalStateException("消息归一序列化失败", e);
        }
    }

    private static BuzhouMessage deserialize(String json) {
        try {
            Map<String, Object> normalized = MAPPER.readValue(json,
                    new TypeReference<Map<String, Object>>() {
                    });
            return new BuzhouMessage(
                    str(normalized.get("id")),
                    str(normalized.get("sessionId")),
                    intOf(normalized.get("turnSeq")),
                    intOf(normalized.get("seqInTurn")),
                    normalized.get("role") == null ? null
                            : Role.valueOf(str(normalized.get("role"))),
                    str(normalized.get("content")),
                    toolCallsFromMaps(normalized.get("toolCalls")),
                    str(normalized.get("toolCallId")),
                    str(normalized.get("reasoningContent")),
                    str(normalized.get("reasoningSignature")),
                    metadataOf(normalized.get("metadata")),
                    normalized.get("createdAt") == null ? null
                            : Instant.ofEpochMilli(longOf(normalized.get("createdAt"))));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("消息反序列化失败（密文解开了但负载非法）", e);
        }
    }

    private static List<Map<String, Object>> toolCallsToMaps(List<ToolCallRecord> toolCalls) {
        List<Map<String, Object>> result = new ArrayList<>(toolCalls.size());
        for (ToolCallRecord call : toolCalls) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", call.id());
            map.put("name", call.name());
            map.put("arguments", call.arguments());
            result.add(map);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<ToolCallRecord> toolCallsFromMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ToolCallRecord> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            result.add(new ToolCallRecord(str(((Map<String, Object>) map).get("id")),
                    str(((Map<String, Object>) map).get("name")),
                    str(((Map<String, Object>) map).get("arguments"))));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataOf(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return Map.copyOf((Map<String, Object>) map);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
