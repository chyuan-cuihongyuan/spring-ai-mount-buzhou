package io.github.chyuan_cuihongyuan.buzhou.resilience.advisor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * 在途模型调用 Future 注册表（spec「补 session.cancel() 的缺口」）。
 *
 * <p>{@code ResilienceAdvisor} 每次（受 deadline 保护地）发起模型调用时注册其 Future；
 * {@code session.cancel()} 经 {@code SessionObserver.onCancel()} 调用 {@link #cancelAll()} 把中断传播进
 * 在途模型调用——与工具调用 {@code HarnessToolCallingManager.cancelInFlight()} 同构，复用同一条中断传播路径。
 *
 * <p>会话内模型调用串行（一次 {@code chat()} 单活跃），故通常至多一个在途 Future；
 * 用 {@link CopyOnWriteArrayList} 容纳极端并发场景。
 */
public class ModelCallInFlight {

    private final List<Future<?>> futures = new CopyOnWriteArrayList<>();

    public void register(Future<?> future) {
        futures.add(future);
    }

    public void unregister(Future<?> future) {
        futures.remove(future);
    }

    /** 中断全部在途模型调用（{@code cancel(true)} 把中断传播进执行线程）。 */
    public void cancelAll() {
        futures.forEach(f -> f.cancel(true));
    }
}
