package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 事务计量装饰器（spec 1416 / T2133 / impl 1069）——PostgreSQL
 * pg_stat_database（xact_commit/xact_commit 比例是健康第一读数）+
 * Seata 事务度量思想：{@link UnitOfWork} 是存储侧事务 SPI（JDBC/Redis/内存
 * 多实现），但「每秒多少事务、多少失败、败在哪个异常类」无任何读面。
 *
 * <p>opt-in 装饰器：包住宿主既有 UnitOfWork（不替换实现——委托逐方法透传），
 * begun/completed/failed 三总量 + 失败异常类 Top 榜（有界 8——基数纪律）。
 * 守恒式：{@code begun = completed + failed + inFlight}；受检异常不捕获
 * （SPI 契约只声明 RuntimeException 路径）。嵌套 {@link Snapshot} +
 * {@link #resetForTest()}。
 */
public final class InstrumentedUnitOfWork implements UnitOfWork {

    /** 失败异常类 Top 榜容量（基数纪律）。 */
    static final int FAILURE_CLASS_CAPACITY = 8;

    private final UnitOfWork delegate;
    private final AtomicLong begun = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong inFlight = new AtomicLong();
    private final Map<String, AtomicLong> failureClasses = new LinkedHashMap<>();

    /** 包住宿主既有实现（委托不为 null——fail-fast）。 */
    public InstrumentedUnitOfWork(UnitOfWork delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate 不得为 null");
        }
        this.delegate = delegate;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> work) {
        begun.incrementAndGet();
        inFlight.incrementAndGet();
        try {
            T result = delegate.executeInTransaction(work);
            completed.incrementAndGet();
            return result;
        } catch (RuntimeException e) {
            recordFailure(e);
            throw e;
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Override
    public <T> T executeInTransaction(String sessionId, Supplier<T> work) {
        begun.incrementAndGet();
        inFlight.incrementAndGet();
        try {
            T result = delegate.executeInTransaction(sessionId, work);
            completed.incrementAndGet();
            return result;
        } catch (RuntimeException e) {
            recordFailure(e);
            throw e;
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        delegate.deleteSession(sessionId);
    }

    /** 失败异常类记账（简单类名；有界 Top——超出容量的并桶计 OTHERS）。 */
    private void recordFailure(RuntimeException e) {
        failed.incrementAndGet();
        String cls = e.getClass().getSimpleName();
        synchronized (failureClasses) {
            if (!failureClasses.containsKey(cls) && failureClasses.size() >= FAILURE_CLASS_CAPACITY) {
                cls = "OTHERS";
            }
            failureClasses.computeIfAbsent(cls, k -> new AtomicLong()).incrementAndGet();
        }
    }

    /** 只读快照：三总量 + 在途 + 失败异常类 Top（次数降序）。 */
    public Snapshot stats() {
        List<Map.Entry<String, Long>> classes;
        synchronized (failureClasses) {
            classes = failureClasses.entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .toList();
        }
        return new Snapshot(begun.get(), completed.get(), failed.get(),
                inFlight.get(), classes);
    }

    /** 测试归零口（装饰器实例级——不影响 delegate）。 */
    public void resetForTest() {
        begun.set(0);
        completed.set(0);
        failed.set(0);
        inFlight.set(0);
        synchronized (failureClasses) {
            failureClasses.clear();
        }
    }

    /**
     * @param begun          累计开启事务数
     * @param completed      正常完成数
     * @param failed         异常失败数（异常原样上抛）
     * @param inFlight       快照时刻在途数（守恒：begun = completed + failed + inFlight）
     * @param failureClasses 失败异常类 Top（次数降序平名典序；容量 8 超出并 OTHERS）
     */
    public record Snapshot(long begun, long completed, long failed, long inFlight,
                           List<Map.Entry<String, Long>> failureClasses) {
    }
}
