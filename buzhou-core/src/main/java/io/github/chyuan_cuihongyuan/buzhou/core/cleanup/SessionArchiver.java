package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 会话归档冷层（spec 97 §A / T363，SessionCleaner 的前置安全网）：删除前把会话
 * 三槽（消息/摘要/state）快照为单个归档 JSON 落合成会话 {@code __buzhou.archive__}
 * （键 {@code archive.<sessionId>}；{@code __buzhou.*} 前缀天然豁免 fsck 会话全集
 * ——同 webhook/eval 先例），再经 {@link SessionCleaner} 级联删除。
 * {@link #restore} 读归档回放三槽（原键原值），归档键随后删除。
 *
 * <p><b>与导出/导入（spec 28）的差异</b>：那是活会话跨环境迁移（Id 重映射语义）；
 * 本类是同环境冷存档（原键原值回放，零语义转换）。空会话（无消息且无摘要）不归档
 * 返回 false——诚实：无可归档内容时不动也不报错。
 */
public final class SessionArchiver {

    /** 合成会话 Id（fsck 天然豁免）。 */
    public static final String ARCHIVE_SESSION_ID = "__buzhou.archive__";
    /** 归档键前缀（健康面 countByPrefix 复用——spec 102 §A / T379）。 */
    public static final String ARCHIVE_PREFIX = "archive.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // Instant ISO-8601 编解码（jackson-databind 自带扩展点——core 不假定 jsr310，
        // WebhookOutbox epoch-millis 同款纪律的字符串版：归档 JSON 人可读）
        com.fasterxml.jackson.databind.module.SimpleModule instantModule =
                new com.fasterxml.jackson.databind.module.SimpleModule();
        instantModule.addSerializer(Instant.class, new com.fasterxml.jackson.databind.JsonSerializer<>() {
            @Override
            public void serialize(Instant value, com.fasterxml.jackson.core.JsonGenerator gen,
                    com.fasterxml.jackson.databind.SerializerProvider serializers) throws java.io.IOException {
                gen.writeString(value.toString());
            }
        });
        instantModule.addDeserializer(Instant.class, new com.fasterxml.jackson.databind.JsonDeserializer<>() {
            @Override
            public Instant deserialize(com.fasterxml.jackson.core.JsonParser parser,
                    com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                return Instant.parse(parser.getText());
            }
        });
        MAPPER.registerModule(instantModule);
    }

    private final BuzhouStores stores;
    private final SessionCleaner cleaner;

    public SessionArchiver(BuzhouStores stores, SessionCleaner cleaner) {
        this.stores = stores;
        this.cleaner = cleaner;
    }

    /**
     * 归档并删除（快照 → 级联清理）。快照编码失败 fail-fast（不删——宁可保留原会话
     * 也不冒数据丢失风险）；空会话返回 false。
     */
    public boolean archive(String sessionId) {
        List<BuzhouMessage> messages = stores.messageStore().load(sessionId);
        Optional<StructuredSummary> summary = stores.summaryStore().latest(sessionId);
        if (messages.isEmpty() && summary.isEmpty()) {
            return false; // 空会话：无可归档内容（诚实不动）
        }
        ArchiveEntry entry = new ArchiveEntry(sessionId, Instant.now(), messages,
                summary.orElse(null), stores.sessionStateStore().getAll(sessionId));
        String json = encode(entry);
        stores.sessionStateStore().put(ARCHIVE_SESSION_ID,
                new StateEntry(ARCHIVE_PREFIX + sessionId, json, "session-archiver",
                        0, null, Instant.now()));
        cleaner.deleteSession(sessionId);
        return true;
    }

    /** 从归档回放三槽（原键原值）；归档键随后删除。无归档 = false。 */
    public boolean restore(String sessionId) {
        Optional<StateEntry> stored = stores.sessionStateStore()
                .get(ARCHIVE_SESSION_ID, ARCHIVE_PREFIX + sessionId);
        if (stored.isEmpty()) {
            return false;
        }
        ArchiveEntry entry = decode(stored.get().value());
        if (!entry.messages().isEmpty()) {
            stores.messageStore().append(sessionId, entry.messages());
        }
        if (entry.summary() != null) {
            stores.summaryStore().save(sessionId, entry.summary());
        }
        for (StateEntry state : entry.states().values()) {
            stores.sessionStateStore().put(sessionId, state);
        }
        stores.sessionStateStore().delete(ARCHIVE_SESSION_ID, ARCHIVE_PREFIX + sessionId);
        return true;
    }

    /** 归档清单（sessionId 字典序）。 */
    public List<String> archived() {
        List<String> out = new ArrayList<>();
        stores.sessionStateStore().scanByPrefix(ARCHIVE_SESSION_ID, ARCHIVE_PREFIX)
                .keySet().forEach(key -> out.add(key.substring(ARCHIVE_PREFIX.length())));
        out.sort(String::compareTo);
        return out;
    }

    /**
     * 归档 TTL 清理（spec 103 §A / T381，spec 102 fog 后半场）：删除归档时间早于
     * {@code now - ttl} 的归档（冷层不是永久层——合规期过后让位容量）。逐条独立
     * 删除（单条解析失败跳过不阻断）；返回删除数。ttl ≤ 0 = 清全部（显式全清语义）。
     */
    public int purgeExpired(java.time.Duration ttl, java.time.Instant now) {
        java.time.Instant cutoff = ttl == null || ttl.isZero() || ttl.isNegative()
                ? java.time.Instant.MAX : now.minus(ttl);
        int purged = 0;
        for (Map.Entry<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry> e
                : stores.sessionStateStore().scanByPrefix(ARCHIVE_SESSION_ID, ARCHIVE_PREFIX)
                        .entrySet()) {
            ArchiveEntry entry;
            try {
                entry = decode(e.getValue().value());
            } catch (RuntimeException parseFailure) {
                continue; // 损坏归档跳过（不阻断批次；修复走手工删）
            }
            if (entry.archivedAt().isBefore(cutoff)) {
                stores.sessionStateStore().delete(ARCHIVE_SESSION_ID, e.getKey());
                purged++;
            }
        }
        return purged;
    }

    /** 归档详情行（spec 120 §A / T421：合规审计——谁在何时归档、规模几何）。 */
    public record ArchivedDetail(String sessionId, Instant archivedAt,
                                 int messageCount, int stateCount) {
    }

    /**
     * 归档详情清单（spec 120 §A / T421：合规审计面）：archivedAt 倒序；损坏归档
     * 以 messageCount=-1 行占位（可见而非静默跳过——修复走手工删）。
     */
    public List<ArchivedDetail> archivedDetailed() {
        List<ArchivedDetail> out = new ArrayList<>();
        stores.sessionStateStore().scanByPrefix(ARCHIVE_SESSION_ID, ARCHIVE_PREFIX)
                .forEach((key, entry) -> {
                    String sessionId = key.substring(ARCHIVE_PREFIX.length());
                    try {
                        ArchiveEntry parsed = decode(entry.value());
                        out.add(new ArchivedDetail(sessionId, parsed.archivedAt(),
                                parsed.messages().size(), parsed.states().size()));
                    } catch (RuntimeException parseFailure) {
                        out.add(new ArchivedDetail(sessionId, Instant.EPOCH, -1, 0));
                    }
                });
        out.sort(java.util.Comparator.comparing(ArchivedDetail::archivedAt).reversed());
        return out;
    }

    /** 归档快照（单会话三槽 + 时间戳）。 */
    public record ArchiveEntry(String sessionId, Instant archivedAt,
                               List<BuzhouMessage> messages, StructuredSummary summary,
                               java.util.Map<String, StateEntry> states) {
    }

    private static String encode(ArchiveEntry entry) {
        try {
            return MAPPER.writeValueAsString(entry);
        } catch (Exception e) {
            // 快照失败 fail-fast：调用方收到异常、原会话保留（不删——安全优先）
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION,
                    "会话归档快照编码失败：" + entry.sessionId() + "（" + e.getMessage() + "）", e);
        }
    }

    private static ArchiveEntry decode(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BuzhouException(ErrorCode.DATA_CORRUPTION,
                    "会话归档解析失败（" + e.getMessage() + "）", e);
        }
    }
}
