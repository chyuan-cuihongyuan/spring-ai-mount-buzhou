package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 观测类写失败策略装饰器（spec 13 §stores-7 / ticket 32，
 * {@code buzhou.store.write-failure-policy = FAIL_TURN | DEGRADE}）。
 *
 * <p><b>DEGRADE 边界</b>：只装饰观测类写（saveSpans / saveEvents / saveInjectionSnapshot
 * ——可再生数据：缺失仅损失可观测性，不损事实）；读路径与事实类写
 * （message / summary / state / lease）不经过本装饰器，任何策略下失败照常抛出
 * ——事实不可静默丢。FAIL_TURN 模式为透明直通（异常原样上抛）。
 *
 * <p>降级不静默：每次降级 WARN 日志 + {@link #degradedWriteCount()} 计数暴露。
 */
public class DegradingObservabilityStore implements ObservabilityStore {

    private static final Logger LOG = LoggerFactory.getLogger(DegradingObservabilityStore.class);

    private final ObservabilityStore delegate;
    private final WriteFailurePolicy policy;
    private final AtomicLong degradedWrites = new AtomicLong();

    public DegradingObservabilityStore(ObservabilityStore delegate, WriteFailurePolicy policy) {
        this.delegate = delegate;
        this.policy = policy == null ? WriteFailurePolicy.FAIL_TURN : policy;
    }

    @Override
    public void saveSpans(List<SpanRecord> spans) {
        String sessionId = spans == null || spans.isEmpty() ? "n/a" : spans.getFirst().sessionId();
        runDegradable(sessionId, "saveSpans", () -> {
            delegate.saveSpans(spans);
            return null;
        });
    }

    @Override
    public void saveEvents(List<EventRecord> events) {
        String sessionId = events == null || events.isEmpty() ? "n/a" : events.getFirst().sessionId();
        runDegradable(sessionId, "saveEvents", () -> {
            delegate.saveEvents(events);
            return null;
        });
    }

    @Override
    public void saveInjectionSnapshot(InjectionSnapshot snapshot) {
        runDegradable(snapshot.sessionId(), "saveInjectionSnapshot", () -> {
            delegate.saveInjectionSnapshot(snapshot);
            return null;
        });
    }

    @Override
    public List<SpanRecord> spansOfSession(String sessionId) {
        return delegate.spansOfSession(sessionId);
    }

    @Override
    public List<EventRecord> eventsOfSession(String sessionId) {
        return delegate.eventsOfSession(sessionId);
    }

    @Override
    public Optional<InjectionSnapshot> injectionSnapshot(String sessionId, int turnSeq) {
        return delegate.injectionSnapshot(sessionId, turnSeq);
    }

    @Override
    public List<SessionSummary> listSessionSummaries(String cursor, int size) {
        return delegate.listSessionSummaries(cursor, size);
    }

    @Override
    public List<EventRecord> eventsOfSpan(String spanId) {
        return delegate.eventsOfSpan(spanId);
    }

    /**
     * impl-35 / spec 13 §stores-6：级联删走同款降级口径——观测类数据可再生，DEGRADE 下
     * 删除失败 WARN + 计数继续（SessionCleaner 的失败聚合因此不会观测清理失败而炸级联）。
     */
    @Override
    public void deleteSession(String sessionId) {
        runDegradable(sessionId, "deleteSession", () -> {
            delegate.deleteSession(sessionId);
            return null;
        });
    }

    /** DEGRADE 时吞掉观测类写失败并计数；FAIL_TURN 原样抛。 */
    private <T> T runDegradable(String sessionId, String operation, java.util.function.Supplier<T> write) {
        if (policy != WriteFailurePolicy.DEGRADE) {
            return write.get();
        }
        try {
            return write.get();
        } catch (RuntimeException e) {
            long count = degradedWrites.incrementAndGet();
            LOG.warn("观测类写降级继续(sessionId={}, operation={}, degradedWrites={}, 原因={})",
                    sessionId, operation, count, e.toString(), e);
            return null;
        }
    }

    /** 累计降级（跳过）的观测类写次数（丢弃不可静默——测试与指标可断言）。 */
    public long degradedWriteCount() {
        return degradedWrites.get();
    }
}
