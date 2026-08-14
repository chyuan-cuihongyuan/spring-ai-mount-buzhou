package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * memory 后台任务生命周期（impl-30 / spec 13 §core-1）：持有
 * {@link MemoryModule#configure} 产出及其登记的模块自有资源（sleep-time 整理调度器），
 * stop 时逐资源关闭（core 停机会话之后才轮到本层——phase 见
 * {@link BuzhouLifecyclePhases#MEMORY}）。
 *
 * <p>诚实边界：本片只做「close 接线」——{@code SleepTimeScheduler.close()} 即
 * {@code shutdownNow()}（丢弃 pending 整理任务）；pending 队列上限、优雅排空、
 * 会话结束摘除等深度治理属切片 38。
 */
public class MemoryModuleLifecycle implements SmartLifecycle {

    private static final System.Logger LOGGER =
            System.getLogger(MemoryModuleLifecycle.class.getName());

    private final RuntimeConfig runtimeConfig;
    private final List<AutoCloseable> moduleOwnedResources;
    private final AtomicBoolean running = new AtomicBoolean();

    public MemoryModuleLifecycle(Map<String, Object> ymlConfig, BuzhouStores stores,
                                 ChatModel mainModel, ChatModel summaryModel,
                                 AttachmentRenderer attachmentRenderer,
                                 SkillCatalogRenderer skillCatalogRenderer) {
        this.moduleOwnedResources = new ArrayList<>();
        this.runtimeConfig = MemoryModule.configure(ymlConfig, stores, mainModel, summaryModel,
                attachmentRenderer, skillCatalogRenderer, moduleOwnedResources);
    }

    /** 装配产出（Spring 装配注册为 {@code RuntimeConfig} bean 的来源）。 */
    public RuntimeConfig runtimeConfig() {
        return runtimeConfig;
    }

    /** 已登记的模块自有资源数（测试可观测性）。 */
    public int managedResourceCount() {
        return moduleOwnedResources.size();
    }

    @Override
    public void start() {
        running.set(true);
    }

    /** 逐资源关闭（单个失败不跳过其余——日志 ERROR 后继续，停机尽力而为）。 */
    @Override
    public void stop() {
        for (AutoCloseable resource : moduleOwnedResources) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.ERROR, "memory 停机关闭模块资源失败（继续关闭其余）", e);
            }
        }
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return BuzhouLifecyclePhases.MEMORY;
    }
}
