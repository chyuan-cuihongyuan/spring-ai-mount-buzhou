package io.github.chyuan_cuihongyuan.buzhou.resilience.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;

import java.util.concurrent.ExecutorService;

/**
 * 韧性层会话生命周期观察者（经 {@code SessionAssemblyContext.addObserver} 注册）。
 *
 * <ul>
 *   <li>{@code onCancel()}：中断在途模型调用——补 {@code session.cancel()} 只中断工具、不触及模型调用的漏网
 *       （{@link ModelCallInFlight#cancelAll()} 与 deadline 共用同一条 Future.cancel(true) 中断路径）。</li>
 *   <li>{@code onClose()}：关闭 deadline 执行器、再兜底中断在途调用，防资源泄漏。</li>
 * </ul>
 */
public class ResilienceSessionObserver implements SessionObserver {

    private final ExecutorService executor;
    private final ModelCallInFlight inFlight;

    public ResilienceSessionObserver(ExecutorService executor, ModelCallInFlight inFlight) {
        this.executor = executor;
        this.inFlight = inFlight;
    }

    @Override
    public void onCancel() {
        inFlight.cancelAll();
    }

    @Override
    public void onClose() {
        inFlight.cancelAll();
        executor.shutdownNow();
    }
}
