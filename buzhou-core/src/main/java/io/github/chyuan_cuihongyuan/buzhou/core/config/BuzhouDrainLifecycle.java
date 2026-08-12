package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.DrainResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * drain 的 Spring 生命周期触发器（spec「06 优雅停机 · SmartLifecycle 装配」）。
 *
 * <p>Spring 停机时调用 {@link #stop(Runnable)}，触发与编程式入口 {@link AgentRuntime#drain(Duration)}
 * <b>同一</b> drain 编排实现——Spring 只是触发器，编排逻辑全部在 {@code DefaultAgentRuntime}。
 *
 * <p>相位定值 {@link SmartLifecycle#DEFAULT_PHASE}（Boot 4 最高相位）：
 * <ul>
 *   <li>先于 web 容器优雅停机（{@code WebServerGracefulLifecycle} 相位 {@code DEFAULT_PHASE - 100}）
 *       停止——drain 期间 web 仍存活，新 HTTP 请求经 spawn 拒新异常（{@code RuntimeDrainingException}）路由，
 *       在途请求的当前轮次被 drain 等完或超时强杀。</li>
 *   <li>先于观测异步管线排空停止——drain 事件全部进入观测管线后再排空，不丢事件。</li>
 * </ul>
 *
 * <p>幂等：重复 stop / 停机中再收信号复用 {@code DefaultAgentRuntime} 的 drain 幂等（首次结果共享）。
 */
public class BuzhouDrainLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(BuzhouDrainLifecycle.class);

    /** drain 相位（Boot 4 SmartLifecycle.DEFAULT_PHASE）：最高相位，先于 web 容器与观测管线停止。 */
    public static final int PHASE = SmartLifecycle.DEFAULT_PHASE;

    private final AgentRuntime runtime;
    private final Duration drainTimeout;
    private final AtomicBoolean running = new AtomicBoolean();

    public BuzhouDrainLifecycle(AgentRuntime runtime, Duration drainTimeout) {
        this.runtime = runtime;
        this.drainTimeout = drainTimeout;
    }

    /** drain 总预算（装配层派生：buzhou.shutdown.drain-timeout > spring.lifecycle.timeout-per-shutdown-phase）。 */
    public Duration drainTimeout() {
        return drainTimeout;
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        stop(() -> {
        });
    }

    @Override
    public void stop(Runnable callback) {
        running.set(false);
        try {
            DrainResult result = runtime.drain(drainTimeout);
            log.info("drain 完成: drained={}, force-killed={}, duration={}ms",
                    result.drainedCount(), result.forceKilledCount(), result.totalDuration().toMillis());
        } catch (RuntimeException e) {
            log.warn("drain 编排异常（停机继续）", e);
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
