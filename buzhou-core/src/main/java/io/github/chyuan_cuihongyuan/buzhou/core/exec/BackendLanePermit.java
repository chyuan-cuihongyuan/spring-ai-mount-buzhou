package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LaneStateBackend;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 后端泳道许可适配（spec 316 / T623）：把 {@link LaneStateBackend} 的即时
 * tryAcquire 适配为带超时的阻塞取用——200ms 步进轮询（粗粒度泳道延迟容忍；
 * Redis 阻塞语义无原生等价）。后端异常按满道处理（fail-closed：资源分道面
 * 宁可排队/超时不可打穿）。
 */
public final class BackendLanePermit {

    /** 轮询步进（粗粒度泳道延迟容忍）。 */
    static final long POLL_STEP_MILLIS = 200L;

    private final LaneStateBackend backend;
    private final String laneName;
    private final int permits;

    private BackendLanePermit(LaneStateBackend backend, String laneName, int permits) {
        this.backend = backend;
        this.laneName = laneName;
        this.permits = permits;
    }

    /** 构造（permits ≥ 1）。 */
    public static BackendLanePermit of(LaneStateBackend backend, String laneName, int permits) {
        if (backend == null || laneName == null || laneName.isBlank() || permits < 1) {
            throw new IllegalArgumentException("backend/laneName 非空、permits>=1");
        }
        return new BackendLanePermit(backend, laneName, permits);
    }

    /**
     * 带超时取许可（步进轮询；中断传播）。
     *
     * @return 是否取得
     */
    public boolean tryAcquire(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            try {
                if (backend.tryAcquire(laneName, permits)) {
                    return true;
                }
            } catch (RuntimeException e) {
                return false; // fail-closed：后端不可达按满道（不打穿下游）
            }
            if (System.nanoTime() >= deadline) {
                return false;
            }
            TimeUnit.MILLISECONDS.sleep(POLL_STEP_MILLIS);
        }
    }

    /** 归还（异常吞——release 失败靠 reset 运维兜底）。 */
    public void release() {
        try {
            backend.release(laneName);
        } catch (RuntimeException ignored) {
            // 崩溃泄漏诚实边界（spec 316）：reset(lane) 运维清零
        }
    }

    /** 泳道名（观测面）。 */
    public String laneName() {
        return laneName;
    }
}
