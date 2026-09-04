package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 摘要槽静态加密装饰器（spec 336 / T663——333 同通道同纪律扩散）：
 * save 时真 {@link StructuredSummary}（sections 全量+tokenEstimate+
 * createdAt）序列化加密成<b>载体摘要</b>——sections 单结构键
 * {@code __envelope__} 承载信封，sessionId/version/tokenEstimate/
 * createdAt 明文供路由；version 真值由底层 save 原子 UPSERT 分配
 * （spec 32 并发语义不破），还原时以底层版本为准。
 *
 * <p><b>AAD 绑定</b> sessionId + createdAt——路由字段被篡改即解密失败；
 * <b>透传兼容</b>旧明文摘要（sections 无结构键）原样返回；<b>幂等安全</b>
 * 已是信封不再包装。篡改宁可炸不可静默（333 同纪律）。
 */
public final class EncryptingSummaryStore implements SummaryStore {

    /** 载体结构键（结构标记，非敏感内容）。 */
    public static final String ENVELOPE_SECTION = "__envelope__";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SummaryStore delegate;
    private final EnvelopeCipher cipher;

    public EncryptingSummaryStore(SummaryStore delegate, EnvelopeCipher cipher) {
        this.delegate = java.util.Objects.requireNonNull(delegate);
        this.cipher = java.util.Objects.requireNonNull(cipher);
    }

    @Override
    public long save(String sessionId, StructuredSummary summary) {
        return delegate.save(sessionId, toCarrier(summary));
    }

    @Override
    public Optional<StructuredSummary> latest(String sessionId) {
        return delegate.latest(sessionId).map(this::fromCarrier);
    }

    @Override
    public List<StructuredSummary> history(String sessionId, int limit) {
        return delegate.history(sessionId, limit).stream().map(this::fromCarrier).toList();
    }

    @Override
    public void deleteSession(String sessionId) {
        delegate.deleteSession(sessionId);
    }

    @Override
    public int pruneVersions(int keepLatest) {
        return delegate.pruneVersions(keepLatest);
    }

    /** 被装饰的底层 store（观测/装配面）。 */
    public SummaryStore delegate() {
        return delegate;
    }

    // ---- 载体往返 ----

    private StructuredSummary toCarrier(StructuredSummary summary) {
        String existing = summary == null ? null : summary.sections().get(ENVELOPE_SECTION);
        if (existing != null && EnvelopeCipher.isEnvelope(existing)
                && summary.sections().size() == 1) {
            return summary; // 已是载体——不再包装（幂等安全）
        }
        String envelope = cipher.encrypt(serialize(summary),
                aadOf(summary.sessionId(), summary.createdAt()));
        return new StructuredSummary(summary.sessionId(), 0L,
                Map.of(ENVELOPE_SECTION, envelope), summary.tokenEstimate(),
                summary.createdAt());
    }

    private StructuredSummary fromCarrier(StructuredSummary stored) {
        String envelope = stored.sections().get(ENVELOPE_SECTION);
        if (envelope == null || !EnvelopeCipher.isEnvelope(envelope)) {
            return stored; // 旧明文透传（迁移友好）
        }
        StructuredSummary decrypted = deserialize(
                cipher.decrypt(envelope, aadOf(stored.sessionId(), stored.createdAt())));
        // 版本以底层为准（载体 0 占位——真实版本由底层 UPSERT 分配）
        return new StructuredSummary(stored.sessionId(), stored.version(),
                decrypted.sections(), decrypted.tokenEstimate(), decrypted.createdAt());
    }

    private static String aadOf(String sessionId, Instant createdAt) {
        return sessionId + "\n" + (createdAt == null ? 0L : createdAt.toEpochMilli());
    }

    // ---- 归一序列化（与 333 同法——不依赖 jackson-jsr310） ----

    private static String serialize(StructuredSummary summary) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("sections", summary.sections());
        normalized.put("tokenEstimate", summary.tokenEstimate());
        normalized.put("createdAt", summary.createdAt() == null
                ? null : summary.createdAt().toEpochMilli());
        try {
            return MAPPER.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new IllegalStateException("摘要归一序列化失败", e);
        }
    }

    private static StructuredSummary deserialize(String json) {
        try {
            Map<String, Object> normalized = MAPPER.readValue(json,
                    new TypeReference<Map<String, Object>>() {
                    });
            @SuppressWarnings("unchecked")
            Map<String, String> sections = (Map<String, String>) normalized.get("sections");
            Object tokenEstimate = normalized.get("tokenEstimate");
            Object createdAt = normalized.get("createdAt");
            return new StructuredSummary(null, 0L,
                    sections == null ? Map.of() : Map.copyOf(sections),
                    tokenEstimate instanceof Number number ? number.intValue() : 0,
                    createdAt == null ? null : Instant.ofEpochMilli(
                            ((Number) createdAt).longValue()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("摘要反序列化失败（密文解开了但负载非法）", e);
        }
    }
}
