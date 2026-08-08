package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link ToolSetSpecStore} 内存默认实现：写后下一轮轮询即推送（同 DB 改配路径）。
 * 供测试与无持久层场景；生产由持久化模块提供实现。
 */
public class InMemoryToolSetSpecStore implements ToolSetSpecStore {

    private volatile List<ToolSetSpec> specs = List.of();
    private final CopyOnWriteArrayList<Runnable> writeListeners = new CopyOnWriteArrayList<>();

    @Override
    public List<ToolSetSpec> loadAll() {
        return specs;
    }

    /** 整体替换清单（模拟后台改配）；写后立即通知 {@link DbToolSetProvider} 免等轮询。 */
    public void replaceAll(List<ToolSetSpec> newSpecs) {
        this.specs = List.copyOf(newSpecs);
        writeListeners.forEach(Runnable::run);
    }

    void addWriteListener(Runnable listener) {
        writeListeners.add(listener);
    }
}
