package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 黄金轨迹断言器（spec 32 / T111 / impl-86）：收集 {@link SessionEvent} 流，断言
 * 机制行为的<b>事件序列</b>——类型顺序 / 间隔约束（A 后必须/不得出现 B）/ 计数 /
 * payload 谓词。与 ScriptedChatModel 同发布于 core test-jar。
 *
 * <p>用法：{@code EventSequenceAssert events = EventSequenceAssert.attach(session);}
 * 驱动会话后 {@code events.assertContainsInOrder("circuit.state-changed", "fallback.switched")}。
 *
 * @since 1.0.0
 */
public final class EventSequenceAssert {

    private final List<SessionEvent> events = new CopyOnWriteArrayList<>();

    private EventSequenceAssert() {
    }

    /** 挂到会话收集事件（会话 addEventListener/removeEventListener 生命周期由会话管）。 */
    public static EventSequenceAssert attach(
            io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession session) {
        EventSequenceAssert collector = new EventSequenceAssert();
        session.addEventListener(collector.events::add);
        return collector;
    }

    /** 挂全局监听（跨会话事件面：session.forked 等发往分支/全局通道的事件）。 */
    public static EventSequenceAssert attachGlobal(
            io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime runtime) {
        if (!(runtime instanceof io.github.chyuan_cuihongyuan.buzhou.core.internal.session
                .DefaultAgentRuntime concrete)) {
            throw new IllegalArgumentException("attachGlobal 需要 DefaultAgentRuntime");
        }
        EventSequenceAssert collector = new EventSequenceAssert();
        concrete.addGlobalEventListener(collector.events::add);
        return collector;
    }

    public List<SessionEvent> events() {
        return List.copyOf(events);
    }

    public List<String> types() {
        return events.stream().map(SessionEvent::type).toList();
    }

    /** 子序列断言：按给定顺序出现（中间允许其他事件）。 */
    public EventSequenceAssert assertContainsInOrder(String... eventTypes) {
        int cursor = 0;
        for (SessionEvent event : events) {
            if (cursor < eventTypes.length && event.type().equals(eventTypes[cursor])) {
                cursor++;
            }
        }
        if (cursor != eventTypes.length) {
            throw new AssertionError("事件序列缺失子序列 " + List.of(eventTypes)
                    + "（实际序列：" + types() + "）");
        }
        return this;
    }

    /** 间隔约束：anchor 之后不得再出现 forbidden。 */
    public EventSequenceAssert assertNeverAfter(String anchor, String forbidden) {
        boolean anchored = false;
        for (SessionEvent event : events) {
            if (event.type().equals(anchor)) {
                anchored = true;
            } else if (anchored && event.type().equals(forbidden)) {
                throw new AssertionError("事件 " + forbidden + " 出现在 " + anchor
                        + " 之后（实际序列：" + types() + "）");
            }
        }
        return this;
    }

    /** 间隔约束：anchor 之后必须出现 required（至迟在会话观测面内）。 */
    public EventSequenceAssert assertFollowedBy(String anchor, String required) {
        return assertContainsInOrder(anchor, required);
    }

    /** 计数断言。 */
    public EventSequenceAssert assertCount(String eventType, int expected) {
        long actual = events.stream().filter(e -> e.type().equals(eventType)).count();
        if (actual != expected) {
            throw new AssertionError("事件 " + eventType + " 计数 " + actual + " ≠ 期望 "
                    + expected + "（实际序列：" + types() + "）");
        }
        return this;
    }

    /** payload 谓词断言（首个命中类型的事件上求值）。 */
    public EventSequenceAssert assertPayload(String eventType, Predicate<java.util.Map<String, Object>> predicate) {
        return events.stream()
                .filter(e -> e.type().equals(eventType))
                .findFirst()
                .map(e -> {
                    if (!predicate.test(e.payload())) {
                        throw new AssertionError("事件 " + eventType + " payload 不满足谓词：" + e.payload());
                    }
                    return this;
                })
                .orElseThrow(() -> new AssertionError("事件 " + eventType + " 未出现（实际序列：" + types() + "）"));
    }

    /**
     * 归一化 payload 断言（spec 607 / T864，approval tests 思想）：首个命中类型的事件
     * payload 经 {@link #normalizePayload} 易变值哨兵化后与期望全等——黄金轨迹可断言
     * payload 结构而不被 UUID/时刻/时长打 flaky。
     */
    public EventSequenceAssert assertPayloadNormalized(String eventType,
            java.util.Map<String, Object> expected) {
        return events.stream()
                .filter(e -> e.type().equals(eventType))
                .findFirst()
                .map(e -> {
                    java.util.Map<String, Object> normalized = normalizePayload(e.payload());
                    if (!expected.equals(normalized)) {
                        throw new AssertionError("事件 " + eventType + " payload 归一化后不符：\n期望 "
                                + expected + "\n实际 " + normalized + "\n原始 " + e.payload());
                    }
                    return this;
                })
                .orElseThrow(() -> new AssertionError("事件 " + eventType + " 未出现（实际序列：" + types() + "）"));
    }

    // ---- 易变值归一化（spec 607 / T864）----

    private static final String UUID_SENTINEL = "<uuid>";
    private static final String INSTANT_SENTINEL = "<instant>";
    private static final String EPOCH_MS_SENTINEL = "<epochMs>";
    private static final String EPOCH_SEC_SENTINEL = "<epochSec>";
    private static final String DURATION_SENTINEL = "<duration>";
    private static final java.util.regex.Pattern UUID_PATTERN = java.util.regex.Pattern.compile(
            "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final java.util.regex.Pattern ISO_INSTANT_PATTERN = java.util.regex.Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$");
    private static final java.util.regex.Pattern ISO_DURATION_PATTERN = java.util.regex.Pattern.compile(
            "^PT[0-9.]+(S|M|H)$");
    private static final long EPOCH_MS_MIN = 1_000_000_000_000L;   // 13 位（2001-09 起）
    private static final long EPOCH_MS_MAX = 9_999_999_999_999L;
    private static final long EPOCH_SEC_MIN = 1_000_000_000L;      // 10 位（2001-09 起）
    private static final long EPOCH_SEC_MAX = 9_999_999_999L;

    /** 深层归一化 payload（Map/List 递归；字符串与数字按易变模式哨兵化）。 */
    public static java.util.Map<String, Object> normalizePayload(java.util.Map<String, Object> payload) {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        payload.forEach((k, v) -> out.put(k, normalizeValue(v)));
        return out;
    }

    /** 单值归一化：UUID/ISO 时刻/ISO 时长 → 哨兵；epoch 量级数字 → 哨兵；容器递归。 */
    public static Object normalizeValue(Object value) {
        if (value instanceof String s) {
            if (UUID_PATTERN.matcher(s).matches()) {
                return UUID_SENTINEL;
            }
            if (ISO_INSTANT_PATTERN.matcher(s).matches()) {
                return INSTANT_SENTINEL;
            }
            if (ISO_DURATION_PATTERN.matcher(s).matches()) {
                return DURATION_SENTINEL;
            }
            return s;
        }
        if (value instanceof Long || value instanceof Integer) {
            long n = ((Number) value).longValue();
            if (n >= EPOCH_MS_MIN && n <= EPOCH_MS_MAX) {
                return EPOCH_MS_SENTINEL;
            }
            if (n >= EPOCH_SEC_MIN && n <= EPOCH_SEC_MAX) {
                return EPOCH_SEC_SENTINEL;
            }
            return value;
        }
        if (value instanceof java.util.Map<?, ?> map) {
            java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), normalizeValue(v)));
            return out;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(EventSequenceAssert::normalizeValue).toList();
        }
        return value;
    }
}
