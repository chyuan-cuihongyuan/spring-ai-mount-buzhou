package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * impl-688 / spec 936：ObservabilityStore 契约校验套件（spec 922/929/930 同构——
 * 契约系列收口最后核心 SPI）。八项语义：span/event 保序、快照写读一致、
 * 未知会话空读、跨会话隔离、deleteSession 幂等、eventsOfSpan 过滤。
 *
 * <p>范式同 {@link SessionLeaseStoreContract}：静态 verify 主源码零 JUnit、
 * 逐项独立收集不抛。
 */
public final class ObservabilityStoreContract {

    /** 单项检查结果。 */
    public record CheckResult(String name, boolean passed, String detail) {
    }

    /** 契约报告（全部检查跑完后聚合——不短路）。 */
    public record ContractReport(int total, int passed, List<CheckResult> checks) {

        /** 全过才 true。 */
        public boolean allPassed() {
            return passed == total;
        }
    }

    private ObservabilityStoreContract() {
    }

    /** 跑全部八项契约检查（store 非空 fail-fast）。 */
    public static ContractReport verify(ObservabilityStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store 必须非空");
        }
        List<CheckResult> checks = new ArrayList<>();
        checks.add(check("spans-of-session-ordered", spansOrdered(store)));
        checks.add(check("events-of-session-ordered", eventsOrdered(store)));
        checks.add(check("injection-snapshot-roundtrip", injectionRoundtrip(store)));
        checks.add(check("unknown-session-empty-reads", unknownSessionEmpty(store)));
        checks.add(check("unknown-turn-snapshot-empty", unknownTurnSnapshotEmpty(store)));
        checks.add(check("cross-session-isolation", crossSessionIsolation(store)));
        checks.add(check("delete-session-idempotent", deleteSessionIdempotent(store)));
        checks.add(check("events-of-span-filtered", eventsOfSpanFiltered(store)));
        long passed = checks.stream().filter(CheckResult::passed).count();
        return new ContractReport(checks.size(), (int) passed, List.copyOf(checks));
    }

    private static CheckResult check(String name, boolean passed) {
        return new CheckResult(name, passed, passed ? "" : "语义不符（见 spec 936 八项定义）");
    }

    private static SpanRecord span(String session, String spanId, int seq) {
        return new SpanRecord(spanId, null, session, 1, "TOOL", "op-" + seq,
                Instant.EPOCH.plusSeconds(seq), Instant.EPOCH.plusSeconds(seq + 1),
                "OK", java.util.Map.of());
    }

    private static EventRecord event(String session, String spanId, int seq) {
        return new EventRecord("ev-" + spanId + "-" + seq, spanId, session, "test.type",
                Instant.EPOCH.plusSeconds(seq), java.util.Map.of());
    }

    /** ① saveSpans 后 spansOfSession 保序返回。 */
    private static boolean spansOrdered(ObservabilityStore store) {
        String s = session("spans");
        store.saveSpans(List.of(span(s, "a1", 1), span(s, "a2", 2), span(s, "a3", 3)));
        List<SpanRecord> spans = store.spansOfSession(s);
        return spans.size() == 3
                && "a1".equals(spans.get(0).spanId())
                && "a3".equals(spans.get(2).spanId());
    }

    /** ② saveEvents 后 eventsOfSession 保序返回。 */
    private static boolean eventsOrdered(ObservabilityStore store) {
        String s = session("events");
        store.saveEvents(List.of(event(s, "e-span", 1), event(s, "e-span", 2)));
        List<EventRecord> events = store.eventsOfSession(s);
        return events.size() == 2 && events.get(0).eventId().endsWith("-1");
    }

    /** ③ injectionSnapshot 写读一致。 */
    private static boolean injectionRoundtrip(ObservabilityStore store) {
        String s = session("snap");
        InjectionSnapshot snapshot = new InjectionSnapshot(s, 2,
                List.of("m1"), List.of(), java.util.Map.of(), "policy-v1", Instant.EPOCH);
        store.saveInjectionSnapshot(snapshot);
        return store.injectionSnapshot(s, 2).isPresent();
    }

    /** ④ 未知会话 span/event 空读。 */
    private static boolean unknownSessionEmpty(ObservabilityStore store) {
        return store.spansOfSession("no-such").isEmpty()
                && store.eventsOfSession("no-such").isEmpty();
    }

    /** ⑤ 未知 turnSeq 快照 empty。 */
    private static boolean unknownTurnSnapshotEmpty(ObservabilityStore store) {
        return store.injectionSnapshot("no-such", 42).isEmpty();
    }

    /** ⑥ 跨会话隔离：s1 写入不影响 s2 空读。 */
    private static boolean crossSessionIsolation(ObservabilityStore store) {
        String s1 = session("iso-a");
        String s2 = session("iso-b");
        store.saveSpans(List.of(span(s1, "iso-1", 1)));
        return store.spansOfSession(s2).isEmpty() && store.spansOfSession(s1).size() == 1;
    }

    /** ⑦ deleteSession 后读空且幂等。 */
    private static boolean deleteSessionIdempotent(ObservabilityStore store) {
        String s = session("delete");
        store.saveSpans(List.of(span(s, "d1", 1)));
        store.deleteSession(s);
        boolean firstClear = store.spansOfSession(s).isEmpty();
        store.deleteSession(s); // 幂等：第二次无操作不抛
        return firstClear && store.spansOfSession(s).isEmpty();
    }

    /** ⑧ eventsOfSpan 按 spanId 过滤。 */
    private static boolean eventsOfSpanFiltered(ObservabilityStore store) {
        String s = session("span-filter");
        store.saveEvents(List.of(event(s, "sp-x", 1), event(s, "sp-x", 2),
                event(s, "sp-y", 3)));
        return store.eventsOfSpan("sp-x").size() == 2
                && store.eventsOfSpan("sp-y").size() == 1;
    }

    private static int counter = 0;

    /** 每项检查独立会话前缀（互不串账——spec 744 教训沿用）。 */
    private static String session(String tag) {
        return "obs-contract-" + tag + "-" + ++counter;
    }
}
