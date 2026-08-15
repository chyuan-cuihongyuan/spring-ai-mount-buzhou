package io.github.chyuan_cuihongyuan.buzhou.memory.consolidation;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * sleep-time 调度器（wayfinder2 impl-11 / T37 / docs/spec/12）：turn 后异步整理的执行基建——
 * JDK21 虚拟线程 + <b>每 session 串行化</b>（同一会话的整理任务按<b>提交顺序</b>互斥执行，
 * 避免 memory 写竞争；Letta sleep-time 与主 agent 共享 block 实体故须串行）。
 *
 * <p>实现为共享虚拟线程池上的<b>串行队列执行器</b>：同 session 严格 FIFO、零常驻额外线程；
 * 不同 session 之间并行。热路径隔离：submit 即返回，整理绝不阻塞主响应。
 *
 * <p>impl-38 / spec 13 §growth-8 深度治理：
 * <ul>
 *   <li><b>pending 队列上限</b>（默认 64/会话）：超限丢弃 + 计数
 *       （{@link #droppedTaskCount()}——丢弃不静默），防失控生产者撑爆内存；</li>
 *   <li><b>会话结束摘除</b>（{@link #removeSession(String)}）：MemoryModule 经会话资源
 *       注册表接线（会话 close 即摘队列，防长跑进程 per-session 表泄漏）；</li>
 *   <li><b>失败指数退避</b>：任务失败重排队尾、按连续失败次数指数退避
 *       （base 起步、×2、封顶 60s 默认；成功即复位）——既有「下个周期重试」语义的兑现；</li>
 *   <li><b>close 排空</b>（impl-30 已接线 memory SmartLifecycle）。</li>
 * </ul>
 */
public final class SleepTimeScheduler implements AutoCloseable {

    /** impl-38：pending 队列默认上限（spec 13 §growth-8：64/会话）。 */
    public static final int DEFAULT_MAX_PENDING_PER_SESSION = 64;
    /** impl-38：失败退避封顶（默认 60s，spec 13 §growth-8）。 */
    static final java.time.Duration DEFAULT_BACKOFF_CAP = java.time.Duration.ofSeconds(60);

    // impl-34 / spec 13 §core-4：线程具名（buzhou-sleep-time-<seq>，thread dump 可归属）
    private final ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(
            io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory.virtual("sleep-time"));
    private final Map<String, SerialQueue> perSession = new ConcurrentHashMap<>();

    private final int maxPendingPerSession;
    private final java.time.Duration backoffBase;
    private final java.time.Duration backoffCap;
    /** spec 44 §A / T159：close 优雅排空预算（缺省 5s）。 */
    private final java.time.Duration closeGrace;
    /** impl-38：超限丢弃的累计任务数（丢弃不静默——测试与运维可断言）。 */
    private final AtomicLong droppedTasks = new AtomicLong();

    public SleepTimeScheduler() {
        this(DEFAULT_MAX_PENDING_PER_SESSION, java.time.Duration.ofSeconds(1), DEFAULT_BACKOFF_CAP);
    }

    /** 测试可注入短退避节奏（base/cap 任意正 Duration）。 */
    public SleepTimeScheduler(int maxPendingPerSession, java.time.Duration backoffBase,
                              java.time.Duration backoffCap) {
        this(maxPendingPerSession, backoffBase, backoffCap, java.time.Duration.ofSeconds(5));
    }

    /** spec 44 §A / T159 / impl-130：带 close 优雅排空预算构造（非正回退 5s）。 */
    public SleepTimeScheduler(int maxPendingPerSession, java.time.Duration backoffBase,
                              java.time.Duration backoffCap, java.time.Duration closeGrace) {
        this.closeGrace = closeGrace == null || !closeGrace.isPositive()
                ? java.time.Duration.ofSeconds(5) : closeGrace;
        this.maxPendingPerSession = Math.max(1, maxPendingPerSession);
        this.backoffBase = backoffBase == null || backoffBase.isZero() || backoffBase.isNegative()
                ? java.time.Duration.ofSeconds(1) : backoffBase;
        this.backoffCap = backoffCap == null || backoffCap.isZero() || backoffCap.isNegative()
                ? DEFAULT_BACKOFF_CAP : backoffCap;
    }

    /**
     * 提交异步整理任务（同 session 按提交顺序串行；不同 session 并行）。
     * impl-38：pending 超上限（默认 64）即丢弃并计数——返回 false 表示本次被丢弃。
     */
    public boolean submit(String sessionId, Runnable task) {
        SerialQueue queue = perSession.computeIfAbsent(sessionId, key -> new SerialQueue());
        if (queue.pending.size() >= maxPendingPerSession) {
            droppedTasks.incrementAndGet();
            System.getLogger(SleepTimeScheduler.class.getName())
                    .log(System.Logger.Level.WARNING,
                            "sleep-time pending 队列超上限丢弃（sessionId={0}, maxPending={1})",
                            sessionId, maxPendingPerSession);
            return false;
        }
        queue.pending.add(task);
        if (queue.draining.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(sessionId, queue));
        }
        return true;
    }

    /** impl-38：会话结束摘除（队列与登记项一并移除；幂等）。 */
    public void removeSession(String sessionId) {
        perSession.remove(sessionId);
    }

    /** impl-38：超限丢弃累计（丢弃不静默）。 */
    public long droppedTaskCount() {
        return droppedTasks.get();
    }

    /** impl-38：在册会话数（测试与运维可观测——验证会话摘除不泄漏）。 */
    public int sessionCount() {
        return perSession.size();
    }

    private static final class SerialQueue {
        final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
        final AtomicBoolean draining = new AtomicBoolean();
        int consecutiveFailures = 0;
    }

    private void drain(String sessionId, SerialQueue queue) {
        try {
            Runnable next;
            while ((next = queue.pending.poll()) != null) {
                try {
                    next.run();
                    queue.consecutiveFailures = 0; // 成功复位退避
                } catch (RuntimeException e) {
                    queue.consecutiveFailures++;
                    // 整理失败不影响会话主链路（韧性：重排队尾 + 指数退避，下个周期重试）
                    System.getLogger(SleepTimeScheduler.class.getName())
                            .log(System.Logger.Level.WARNING,
                                    "sleep-time 整理任务失败（sessionId=" + sessionId + "，第 "
                                            + queue.consecutiveFailures + " 次连续失败，退避 "
                                            + backoffFor(queue.consecutiveFailures) + "）", e);
                    queue.pending.add(next);
                    sleepQuietly(backoffFor(queue.consecutiveFailures));
                }
            }
        } finally {
            queue.draining.set(false);
            // 竞态兜底：置 false 后新提交者可能未触发 drain——若队列非空再抢一次
            if (!queue.pending.isEmpty() && queue.draining.compareAndSet(false, true)) {
                virtualExecutor.execute(() -> drain(sessionId, queue));
            }
        }
    }

    /** 指数退避：base × 2^(failures-1)，封顶 cap（虚拟线程休眠零成本）。 */
    private java.time.Duration backoffFor(int consecutiveFailures) {
        long baseMs = backoffBase.toMillis();
        long capped = backoffCap.toMillis();
        long exponential = baseMs << Math.min(consecutiveFailures - 1, 20);
        return java.time.Duration.ofMillis(Math.min(exponential, capped));
    }

    private static void sleepQuietly(java.time.Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * spec 44 §A / T159 / impl-130：优雅关闭——先 shutdown 排空在途整理任务（有界等待，缺省 5s、
     * 构造可调），超时再 shutdownNow 硬截断。修前 shutdownNow 直接丢弃 pending 摘要任务
     * （Lifecycle javadoc 自认的遗留）。
     */
    @Override
    public void close() {
        virtualExecutor.shutdown();
        try {
            if (!virtualExecutor.awaitTermination(closeGrace.toMillis(), TimeUnit.MILLISECONDS)) {
                virtualExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
