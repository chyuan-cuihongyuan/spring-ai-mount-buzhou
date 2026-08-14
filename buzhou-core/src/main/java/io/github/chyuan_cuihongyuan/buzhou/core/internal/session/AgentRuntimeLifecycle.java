package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * core 会话/执行层的 {@link SmartLifecycle}（impl-30 / spec 13 §core-1 优雅停机与生命周期）。
 *
 * <p>phase = {@link BuzhouLifecyclePhases#CORE}（最大，最先 stop）。stop 委托
 * {@link DefaultAgentRuntime#shutdownGracefully(Duration)}：拒绝新 Turn → 对在途会话发
 * {@code AFTER_CURRENT_TURN} 取消 → 排空等待（预算
 * {@code buzhou.lifecycle.timeout-per-shutdown-phase}，默认 30s）→ 超时硬截断
 * （IMMEDIATE 取消 + executor {@code shutdownNow}）→ 关闭全部会话（租约释放/资源注册表清空）
 * → 停续租守护线程。stop 完成后<b>必回调</b> callback（异常也回调，停机序列尽力而为不上抛）。
 *
 * <p>「没有 stop 直接 destroy」容忍：容器销毁路径由 runtime bean 的显式
 * {@code destroyMethod = "close"} 兜底（{@link DefaultAgentRuntime#close()} 幂等硬截断收尾）；
 * 本类 stop 与 destroy 双触发经 runtime 停机状态机吸收（第二次调用 no-op），无双重副作用。
 */
public class AgentRuntimeLifecycle implements SmartLifecycle {

    private static final System.Logger LOGGER =
            System.getLogger(AgentRuntimeLifecycle.class.getName());

    private final DefaultAgentRuntime runtime;
    private final Duration shutdownTimeout;
    private final AtomicBoolean running = new AtomicBoolean();

    public AgentRuntimeLifecycle(DefaultAgentRuntime runtime, Duration shutdownTimeout) {
        this.runtime = runtime;
        this.shutdownTimeout = shutdownTimeout;
    }

    @Override
    public void start() {
        // runtime 懒启动（首个 spawn 才起续租守护），start 仅翻运行标记
        running.set(true);
    }

    @Override
    public void stop() {
        stop(() -> {
        });
    }

    @Override
    public void stop(Runnable callback) {
        try {
            runtime.shutdownGracefully(shutdownTimeout);
        } catch (RuntimeException e) {
            // 停机序列自身失败（非排空超时——那由 shutdownGracefully 内部硬截断兜底）：
            // 记 ERROR 不上抛，保证 callback 必被调用、容器停机流程不被阻断
            LOGGER.log(System.Logger.Level.ERROR, "core 停机序列异常（尽力而为收尾）", e);
        } finally {
            running.set(false);
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return BuzhouLifecyclePhases.CORE;
    }
}
