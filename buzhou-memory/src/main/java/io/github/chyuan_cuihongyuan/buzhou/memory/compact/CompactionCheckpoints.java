package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 压缩前检查点与三档回滚（wayfinder2 impl-13 / T40 / docs/spec/12 §memory-12，Cline 同思路）：
 * compact/增量摘要<b>提交前</b>把压缩前消息窗不可变快照按水位存入会话 state；
 * 压缩事故后可回滚。
 *
 * <p><b>视图级回滚</b>：MessageStore 为 append-only 事实源（不可改写、天然审计友好），
 * 回滚作用于<b>注入视图</b>——恢复为检查点窗口（原始消息），撤销摘要注入；
 * 持久历史保持连续。三档：
 * <ol>
 *   <li>{@code MESSAGES_ONLY}：注入视图恢复为压缩前窗口；</li>
 *   <li>{@code PLUS_SUMMARY_INVALIDATION}：另将最新摘要标记失效（后续视图不再注入，
 *       直至重新压缩生成新摘要）；</li>
 *   <li>{@code WITH_FACT_LEDGER}：连同事实台账（fact.* state 键）一并回滚——<b>默认关</b>，
 *       仅在确证台账被污染时使用。</li>
 * </ol>
 */
public final class CompactionCheckpoints {

    /** 最新检查点 state 键（值 = JSON：turn + 消息窗序列化，条数封顶防膨胀）。 */
    public static final String STATE_CHECKPOINT = "memory.checkpoint.latest";
    /** 回滚标记 state 键（一次性消费：下一次视图生成即恢复并清除）。 */
    public static final String STATE_ROLLBACK = "memory.rollback";
    /** 摘要失效标记（PLUS_SUMMARY_INVALIDATION 档写入；IVP 检查后跳过摘要注入）。 */
    public static final String STATE_SUMMARY_INVALIDATED = "memory.summary.invalidated";

    /** 单检查点消息条数封顶（状态值不宜无限膨胀；足够覆盖典型压缩窗口）。 */
    public static final int WINDOW_CAP_MESSAGES = 200;

    /** 回滚档位。 */
    public enum RollbackLevel {
        MESSAGES_ONLY,
        PLUS_SUMMARY_INVALIDATION,
        WITH_FACT_LEDGER
    }

    // findAndRegisterModules：接入 jackson-datatype-jsr310（BuzhouMessage.createdAt=Instant）
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final SessionStateStore stateStore;

    public CompactionCheckpoints(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /** 压缩提交前保存检查点（水位 = 即将折叠到的 turn；窗口按末尾 N 条封顶）。 */
    public void save(String sessionId, int cutoffTurn, List<BuzhouMessage> window) {
        try {
            List<BuzhouMessage> capped = window.size() > WINDOW_CAP_MESSAGES
                    ? window.subList(window.size() - WINDOW_CAP_MESSAGES, window.size())
                    : window;
            String payload = MAPPER.writeValueAsString(Map.of(
                    "cutoffTurn", cutoffTurn,
                    "messageCount", capped.size(),
                    "savedAt", Instant.now().toString(),
                    "window", capped.stream().map(CompactionCheckpoints::toMap).toList()));
            stateStore.put(sessionId, new StateEntry(STATE_CHECKPOINT, payload,
                    "CompactionCheckpoints", cutoffTurn, null, Instant.now()));
        } catch (Exception ignored) {
            // 检查点是护栏而非主链路：序列化失败不阻断压缩（尽力而为）
        }
    }

    /** 消息 → 可移植 Map（createdAt 转 ISO 字符串，不依赖 jackson-jsr310）。 */
    private static Map<String, Object> toMap(BuzhouMessage m) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", m.id());
        map.put("sessionId", m.sessionId());
        map.put("turnSeq", m.turnSeq());
        map.put("seqInTurn", m.seqInTurn());
        map.put("role", m.role().name());
        map.put("content", m.content());
        map.put("toolCalls", m.toolCalls());
        map.put("toolCallId", m.toolCallId());
        map.put("reasoningContent", m.reasoningContent());
        map.put("reasoningSignature", m.reasoningSignature());
        map.put("metadata", m.metadata());
        map.put("createdAt", m.createdAt() == null ? null : m.createdAt().toString());
        return map;
    }

    /** 可移植 Map → 消息。 */
    private static BuzhouMessage fromMap(Map<?, ?> map) {
        return new BuzhouMessage(
                (String) map.get("id"),
                (String) map.get("sessionId"),
                ((Number) map.get("turnSeq")).intValue(),
                ((Number) map.get("seqInTurn")).intValue(),
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.valueOf((String) map.get("role")),
                (String) map.get("content"),
                MAPPER.convertValue(map.get("toolCalls"), MAPPER.getTypeFactory()
                        .constructCollectionType(java.util.ArrayList.class,
                                io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord.class)),
                (String) map.get("toolCallId"),
                (String) map.get("reasoningContent"),
                (String) map.get("reasoningSignature"),
                MAPPER.convertValue(map.get("metadata") == null ? Map.of() : map.get("metadata"),
                        MAPPER.getTypeFactory().constructMapType(
                                java.util.LinkedHashMap.class, String.class, Object.class)),
                map.get("createdAt") == null ? null : Instant.parse((String) map.get("createdAt")));
    }

    /** 最新检查点的消息窗（无检查点/解析失败 = empty）。 */
    public java.util.Optional<List<BuzhouMessage>> latestWindow(String sessionId) {
        return stateStore.get(sessionId, STATE_CHECKPOINT).flatMap(entry -> {
            try {
                com.fasterxml.jackson.databind.JsonNode window =
                        MAPPER.readTree(entry.value()).path("window");
                if (!window.isArray() || window.isEmpty()) {
                    return java.util.Optional.empty();
                }
                List<BuzhouMessage> restored = new java.util.ArrayList<>(window.size());
                for (com.fasterxml.jackson.databind.JsonNode node : window) {
                    restored.add(fromMap(MAPPER.convertValue(node,
                            MAPPER.getTypeFactory().constructMapType(
                                    java.util.LinkedHashMap.class, String.class, Object.class))));
                }
                return java.util.Optional.of(restored);
            } catch (Exception e) {
                return java.util.Optional.empty();
            }
        });
    }

    /**
     * 回滚（一次性）：置回滚标记（含档位语义）；档 ≥2 另置摘要失效；档 3 另清事实台账。
     * 下一次注入视图生成时恢复为检查点窗口并消费标记。
     *
     * @return 检查点是否存在（false = 无可回滚内容，未置标记）
     */
    public boolean rollback(String sessionId, RollbackLevel level) {
        if (stateStore.get(sessionId, STATE_CHECKPOINT).isEmpty()) {
            return false;
        }
        stateStore.put(sessionId, new StateEntry(STATE_ROLLBACK, level.name(),
                "CompactionCheckpoints", 0, null, Instant.now()));
        if (level.ordinal() >= RollbackLevel.PLUS_SUMMARY_INVALIDATION.ordinal()) {
            stateStore.put(sessionId, new StateEntry(STATE_SUMMARY_INVALIDATED, "true",
                    "CompactionCheckpoints", 0, null, Instant.now()));
        }
        if (level == RollbackLevel.WITH_FACT_LEDGER) {
            stateStore.getAll(sessionId).keySet().stream()
                    .filter(key -> key.startsWith("fact."))
                    .toList()
                    .forEach(key -> stateStore.delete(sessionId, key));
        }
        return true;
    }

    /**
     * 按 Turn 对齐消费回滚标记：同一 Turn 内的所有视图生成（一次 chat 会触发多次 get）
     * 一致恢复为检查点窗口；<b>下一 Turn 自动失效</b>恢复正常压缩链路。
     *
     * <p>标记值形状：{@code LEVEL} 或 {@code LEVEL|seenTurn=N}（首次命中写回 seenTurn）。
     */
    public java.util.Optional<RollbackLevel> consumeRollbackForTurn(String sessionId, int currentTurn) {
        var marker = stateStore.get(sessionId, STATE_ROLLBACK);
        if (marker.isEmpty()) {
            return java.util.Optional.empty();
        }
        String value = marker.get().value() == null ? "" : marker.get().value();
        int bar = value.indexOf('|');
        String levelPart = bar < 0 ? value : value.substring(0, bar);
        RollbackLevel level;
        try {
            level = RollbackLevel.valueOf(levelPart);
        } catch (IllegalArgumentException e) {
            stateStore.delete(sessionId, STATE_ROLLBACK);
            return java.util.Optional.empty();
        }
        if (bar < 0) {
            // 首次命中：写回 seenTurn，同 Turn 后续 get 保持恢复
            stateStore.put(sessionId, new StateEntry(STATE_ROLLBACK,
                    levelPart + "|seenTurn=" + currentTurn, "CompactionCheckpoints",
                    currentTurn, null, Instant.now()));
            return java.util.Optional.of(level);
        }
        int seenTurn = Integer.parseInt(value.substring(bar + 1).replace("seenTurn=", ""));
        if (seenTurn >= currentTurn) {
            return java.util.Optional.of(level);
        }
        // 下一 Turn：恢复正常链路
        stateStore.delete(sessionId, STATE_ROLLBACK);
        return java.util.Optional.empty();
    }

    /** 摘要是否被失效标记（PLUS_SUMMARY_INVALIDATION 档）。 */
    public static boolean summaryInvalidated(SessionStateStore stateStore, String sessionId) {
        return stateStore.get(sessionId, STATE_SUMMARY_INVALIDATED)
                .map(StateEntry::value)
                .filter("true"::equals)
                .isPresent();
    }

    /** 清除摘要失效标记（重新压缩生成新摘要后调用）。 */
    public static void clearSummaryInvalidation(SessionStateStore stateStore, String sessionId) {
        stateStore.delete(sessionId, STATE_SUMMARY_INVALIDATED);
    }
}
