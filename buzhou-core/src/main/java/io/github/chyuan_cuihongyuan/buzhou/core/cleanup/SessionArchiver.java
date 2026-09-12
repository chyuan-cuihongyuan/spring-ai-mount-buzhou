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
    /**
     * spec 511 / T771：归档完整性校验和命名空间（独立 state id——不与
     * {@code archive.} 前缀同域，countByPrefix/清单扫描语义不变；S3 checksum 思想）。
     */
    public static final String ARCHIVE_CHECKSUM_SESSION_ID = "__buzhou.archive-checksum__";
    public static final String ARCHIVE_CHECKSUM_PREFIX = "checksum.";

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

    /** spec 622 / T894：每会话归档/还原互斥锁（跨会话并行；条目级对象锁——会话数级）。 */
    private final java.util.concurrent.ConcurrentHashMap<String, Object> sessionLocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    private Object lockOf(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId == null ? "" : sessionId, k -> new Object());
    }

    /**
     * 归档并删除（快照 → 级联清理）。快照编码失败 fail-fast（不删——宁可保留原会话
     * 也不冒数据丢失风险）；空会话返回 false。
     *
     * <p>spec 304 / T600：saga 两步（写归档 → 级联删活数据）——级联<b>部分失败上抛</b>
     * （诚实化：不再吞掉返回 true），已成步倒序补偿（归档条目即 undo log：从条目
     * 写回 消息/摘要/状态 三槽）；补偿失败即停止回退——归档键保留（唯一完整副本，
     * 人工介入重试）。
     */
    public boolean archive(String sessionId) {
        // spec 622 / T894：同会话 archive/restore 互斥——并发 archive+restore 交错会把
        // 刚还原的活数据删掉而归档键已被 restore 删除（数据丢失窗）；跨会话不受影响
        synchronized (lockOf(sessionId)) {
            return archiveLocked(sessionId);
        }
    }

    private boolean archiveLocked(String sessionId) {
        List<BuzhouMessage> messages = stores.messageStore().load(sessionId);
        Optional<StructuredSummary> summary = stores.summaryStore().latest(sessionId);
        if (messages.isEmpty() && summary.isEmpty()) {
            return false; // 空会话：无可归档内容（诚实不动）
        }
        ArchiveEntry entry = new ArchiveEntry(sessionId, Instant.now(), messages,
                summary.orElse(null), stores.sessionStateStore().getAll(sessionId));
        String json = encode(entry);
        // 级联删除非原子（部分失败有残余）——残余效果由已成步（archive-write）的补偿
        // 承担：liveTouched 标记后步已动活数据，则从条目写回（undo log）再撤归档键。
        java.util.concurrent.atomic.AtomicBoolean liveTouched =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        // spec 623 / T896：per-session 事务域（跨会话归档并行；同会话已由条目锁串行）
        return io.github.chyuan_cuihongyuan.buzhou.core.transaction.CompensatingBatch.run(
                stores.unitOfWork(), sessionId,
                List.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.transaction.CompensatingBatch.Step.of(
                                "archive-write",
                                () -> {
                                    stores.sessionStateStore().put(ARCHIVE_SESSION_ID,
                                            new StateEntry(ARCHIVE_PREFIX + sessionId, json,
                                                    "session-archiver", 0, null, Instant.now()));
                                    // spec 511 / T771：完整性校验和随条目落盘（sha256 hex）
                                    stores.sessionStateStore().put(ARCHIVE_CHECKSUM_SESSION_ID,
                                            new StateEntry(ARCHIVE_CHECKSUM_PREFIX + sessionId,
                                                    sha256Hex(json), "session-archiver", 0,
                                                    null, Instant.now()));
                                    return entry;
                                },
                                written -> {
                                    if (liveTouched.get()) {
                                        writeBackLive(entry);
                                    }
                                    stores.sessionStateStore()
                                            .delete(ARCHIVE_SESSION_ID, ARCHIVE_PREFIX + sessionId);
                                    stores.sessionStateStore()
                                            .delete(ARCHIVE_CHECKSUM_SESSION_ID, ARCHIVE_CHECKSUM_PREFIX + sessionId);
                                }),
                        io.github.chyuan_cuihongyuan.buzhou.core.transaction.CompensatingBatch.Step.of(
                                "live-delete",
                                () -> {
                                    liveTouched.set(true);
                                    SessionCleanupResult result = cleaner.deleteSession(sessionId);
                                    if (!result.failures().isEmpty()) {
                                        throw new IllegalStateException("归档级联清理部分失败（sessionId="
                                                + sessionId + "，失败目标=" + result.failures().keySet() + "）");
                                    }
                                    return result;
                                },
                                null))) != null;
    }

    /** saga 补偿：从归档条目写回活数据三槽（消息/摘要/状态——undo log 重放）。 */
    private void writeBackLive(ArchiveEntry entry) {
        String sessionId = entry.sessionId();
        if (!entry.messages().isEmpty()) {
            stores.messageStore().append(sessionId, entry.messages());
        }
        if (entry.summary() != null) {
            stores.summaryStore().save(sessionId, entry.summary());
        }
        for (StateEntry state : entry.states().values()) {
            stores.sessionStateStore().put(sessionId, state);
        }
    }

    /** 从归档回放三槽（原键原值）；归档键随后删除。无归档 = false。 */
    public boolean restore(String sessionId) {
        // spec 622 / T894：同会话互斥（见 archive 注释——交错=数据丢失窗）
        synchronized (lockOf(sessionId)) {
            return restoreLocked(sessionId);
        }
    }

    private boolean restoreLocked(String sessionId) {
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
        stores.sessionStateStore().delete(ARCHIVE_CHECKSUM_SESSION_ID, ARCHIVE_CHECKSUM_PREFIX + sessionId);
        return true;
    }

    /**
     * spec 511 / T771：单会话归档完整性校验（S3 checksum 思想——可读≠未被
     * 改）。NO_ARCHIVE=无归档；CORRUPT=JSON 不可解码；NO_CHECKSUM=存量归档
     * （本特性前写入）；CHECKSUM_MISMATCH=内容与校验和不符（冷层被改/衰变）；
     * OK=一致。
     */
    public VerifyResult verify(String sessionId) {
        Optional<StateEntry> stored = stores.sessionStateStore()
                .get(ARCHIVE_SESSION_ID, ARCHIVE_PREFIX + sessionId);
        if (stored.isEmpty()) {
            return new VerifyResult(sessionId, VerifyState.NO_ARCHIVE, null);
        }
        String json = stored.get().value();
        try {
            decode(json);
        } catch (RuntimeException e) {
            return new VerifyResult(sessionId, VerifyState.CORRUPT, null);
        }
        Optional<StateEntry> checksum = stores.sessionStateStore()
                .get(ARCHIVE_CHECKSUM_SESSION_ID, ARCHIVE_CHECKSUM_PREFIX + sessionId);
        if (checksum.isEmpty()) {
            return new VerifyResult(sessionId, VerifyState.NO_CHECKSUM, null);
        }
        String expected = sha256Hex(json);
        return expected.equals(checksum.get().value())
                ? new VerifyResult(sessionId, VerifyState.OK, expected)
                : new VerifyResult(sessionId, VerifyState.CHECKSUM_MISMATCH, checksum.get().value());
    }

    /** 全量校验（archived 清单序——校验面/健康面接用）。 */
    public List<VerifyResult> verifyAll() {
        List<VerifyResult> out = new ArrayList<>();
        for (String sessionId : archived()) {
            out.add(verify(sessionId));
        }
        return out;
    }

    /** 校验结论（detail = 记录在案的校验和值——MISMATCH 时可见冷层现值）。 */
    public record VerifyResult(String sessionId, VerifyState state, String detail) {
    }

    /** 校验状态枚举。 */
    public enum VerifyState { OK, CHECKSUM_MISMATCH, NO_CHECKSUM, CORRUPT, NO_ARCHIVE }

    /** sha256 hex（spec 511 校验和口径）。 */
    static String sha256Hex(String value) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
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
                if (e.getKey().startsWith(ARCHIVE_PREFIX)) {
                    stores.sessionStateStore().delete(ARCHIVE_CHECKSUM_SESSION_ID,
                            ARCHIVE_CHECKSUM_PREFIX + e.getKey().substring(ARCHIVE_PREFIX.length()));
                }
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
