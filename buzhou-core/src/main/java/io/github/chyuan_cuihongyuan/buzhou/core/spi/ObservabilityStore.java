package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.util.List;
import java.util.Optional;

public interface ObservabilityStore {

    void saveSpans(List<SpanRecord> spans);

    void saveEvents(List<EventRecord> events);

    List<SpanRecord> spansOfSession(String sessionId);

    List<EventRecord> eventsOfSession(String sessionId);

    void saveInjectionSnapshot(InjectionSnapshot snapshot);

    Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq);

    /**
     * 会话列表（spec 03 推演 #11，ticket 17 dashboard 数据源）：按最近活跃降序。
     *
     * @param cursor 分页游标（offset 语义的不透明字符串；null/空 = 首页）
     * @param size   页大小
     */
    List<SessionSummary> listSessionSummaries(String cursor, int size);

    /** 单 span 的 Event 流（spec 03 findEventsBySpan 的实现定名，ticket 17 补回）。 */
    List<EventRecord> eventsOfSpan(String spanId);

    /**
     * impl-35 / spec 13 §stores-6：删除该会话的全部观测数据（spans / events /
     * 注入快照 / 会话活跃索引项）。幂等——会话不存在时无操作。默认 no-op
     * （既有实现二进制兼容，由各实现补齐语义）。
     */
    default void deleteSession(String sessionId) {
    }

    /**
     * impl-37 / spec 13 §stores-6：枚举封闭早于 {@code closedBefore} 的会话及其封闭时刻
     * （锚点 = SESSION span 的 endedAt；活动会话永不出现在结果中）。默认空——
     * 无 SESSION span 事实源的实现无「封闭」语义可枚举。
     *
     * @param closedBefore 封闭时刻上界（含之前）
     * @param limit        结果上限（批删限量同源）
     */
    default java.util.List<ClosedSession> listClosedSessions(java.time.Instant closedBefore, int limit) {
        return java.util.List.of();
    }

    /**
     * impl-37 / spec 13 §stores-6：观测 TTL 批删（ClickHouse 低频兑现语义）——删除过期
     * events/spans/snapshots（可再生流水，只删记录不删会话），返回删除条数；
     * 单次批量以 {@code policy.batchSize()} 为限。默认 no-op（返回 0）。
     */
    default int prune(io.github.chyuan_cuihongyuan.buzhou.core.retention.ObservabilityTtl policy) {
        return 0;
    }
}
