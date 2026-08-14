package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * guard 生命周期（impl-30 / spec 13 §core-1）：phase =
 * {@link BuzhouLifecyclePhases#GUARD}（core/memory/spill 之后、store 之前停）。
 *
 * <p><b>诚实边界（本片占位）</b>：审计链（{@code AuditChain}）由应用经
 * {@code SpawnOptions.withListeners(AuditTrailCollector)} 自持、装配面未接线，且为进程内
 * 链——<b>无可 flush 的挂起状态</b>；{@code GuardModule} 产出的 hooks 均无后台任务。
 * 故本 lifecycle 只做 phase 声明与停机占位；审计链 flush 钩子随切片 39
 * （AuditRecordStore 持久化）落地时在此接线。
 */
public class GuardModuleLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean();

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        // 占位：本片无可关闭资源/挂起 flush（见类 Javadoc 诚实边界）；切片 39 的审计 flush 落位于此
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return BuzhouLifecyclePhases.GUARD;
    }
}
