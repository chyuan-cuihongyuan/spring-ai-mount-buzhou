package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * spill 生命周期（impl-30 / spec 13 §core-1）：phase =
 * {@link BuzhouLifecyclePhases#SPILL}（core 停机会话之后、guard/store 之前）。
 *
 * <p><b>诚实边界（本片占位）</b>：spill 侧当前<b>无可关闭资源</b>——
 * {@code HandleLifecycleRegistry} / {@code SessionReadOnlyRegistry} / {@code SemanticChunkIndex}
 * 均为进程内 {@code ConcurrentHashMap}（无 flush 语义），{@code DiskSpillStore} 按操作开关
 * 文件、不持常驻句柄。故本 lifecycle 只做 phase 声明与停机占位；句柄注册表/缓存的
 * flush 语义随切片 38（增长治理：配额、孤儿扫描、LRU embedding 缓存）落地时在此接线。
 */
public class SpillModuleLifecycle implements SmartLifecycle {

    private final AtomicBoolean running = new AtomicBoolean();

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        // 占位：本片无可关闭资源（见类 Javadoc 诚实边界）；切片 38 的缓存/句柄 flush 落位于此
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return BuzhouLifecyclePhases.SPILL;
    }
}
