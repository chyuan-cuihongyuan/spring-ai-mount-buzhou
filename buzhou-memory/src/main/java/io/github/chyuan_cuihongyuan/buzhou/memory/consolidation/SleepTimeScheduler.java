package io.github.chyuan_cuihongyuan.buzhou.memory.consolidation;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * sleep-time 调度器（wayfinder2 impl-11 / T37 / docs/spec/12）：turn 后异步整理的执行基建——
 * JDK21 虚拟线程 + <b>每 session 串行化</b>（同一会话的整理任务按<b>提交顺序</b>互斥执行，
 * 避免 memory 写竞争；Letta sleep-time 与主 agent 共享 block 实体故须串行）。
 *
 * <p>实现为共享虚拟线程池上的<b>串行队列执行器</b>：同 session 严格 FIFO、零常驻额外线程；
 * 不同 session 之间并行。热路径隔离：submit 即返回，整理绝不阻塞主响应。
 */
public final class SleepTimeScheduler implements AutoCloseable {

    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, SerialQueue> perSession = new ConcurrentHashMap<>();

    private static final class SerialQueue {
        final Queue<Runnable> pending = new ConcurrentLinkedQueue<>();
        final AtomicBoolean draining = new AtomicBoolean();
    }

    /** 提交异步整理任务（同 session 按提交顺序串行；不同 session 并行）。 */
    public void submit(String sessionId, Runnable task) {
        SerialQueue queue = perSession.computeIfAbsent(sessionId, key -> new SerialQueue());
        queue.pending.add(task);
        if (queue.draining.compareAndSet(false, true)) {
            virtualExecutor.execute(() -> drain(sessionId, queue));
        }
    }

    private void drain(String sessionId, SerialQueue queue) {
        try {
            Runnable next;
            while ((next = queue.pending.poll()) != null) {
                try {
                    next.run();
                } catch (RuntimeException e) {
                    // 整理失败不影响会话主链路（韧性：记录后继续，下个周期重试）
                    System.getLogger(SleepTimeScheduler.class.getName())
                            .log(System.Logger.Level.WARNING,
                                    "sleep-time 整理任务失败（sessionId=" + sessionId + "）", e);
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

    @Override
    public void close() {
        virtualExecutor.shutdownNow();
    }
}
