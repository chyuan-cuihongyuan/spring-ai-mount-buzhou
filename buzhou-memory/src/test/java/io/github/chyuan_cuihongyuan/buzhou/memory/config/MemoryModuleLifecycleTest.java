package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.consolidation.SleepTimeScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-30 / spec 13 §core-1：memory 停机生命周期接线——
 * {@link MemoryModule#configure} 的资源收集器重载登记 sleep-time 调度器
 * （此前内联创建从不关闭）；{@link MemoryModuleLifecycle} 持 phase（core 之后停）。
 * 深度治理（pending 上限、优雅排空）属切片 38——本片 close 即 shutdownNow。
 */
class MemoryModuleLifecycleTest {

    @Test
    void shouldCollectSleepTimeSchedulerIntoModuleResources_whenConfiguredWithCollector() throws Exception {
        List<AutoCloseable> owned = new ArrayList<>();
        MemoryModule.configure(memoryConfig(Map.of("enabled", true)),
                Buzhou.inMemoryStores(), summaryModel(), summaryModel(), null, null, owned);

        assertThat(owned).hasSize(1);
        assertThat(owned.get(0)).isInstanceOf(SleepTimeScheduler.class);

        // close 后调度器拒绝新任务（关闭真实生效）
        owned.get(0).close();
        SleepTimeScheduler scheduler = (SleepTimeScheduler) owned.get(0);
        assertThatThrownBy(() -> scheduler.submit("closed-session", () -> {
                }))
                .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
    }

    @Test
    void shouldNotCollectScheduler_whenSleepTimeDisabled() {
        List<AutoCloseable> owned = new ArrayList<>();
        MemoryModule.configure(memoryConfig(Map.of("enabled", false)),
                Buzhou.inMemoryStores(), summaryModel(), summaryModel(), null, null, owned);
        assertThat(owned).isEmpty();
    }

    @Test
    void shouldDeclareMemoryPhaseAndCloseResources_whenLifecycleStops() {
        MemoryModuleLifecycle lifecycle = new MemoryModuleLifecycle(
                memoryConfig(Map.of()), Buzhou.inMemoryStores(),
                summaryModel(), summaryModel(), null, null);

        // phase 契约：memory 后台层在 core（MAX）之后停
        assertThat(lifecycle.getPhase()).isEqualTo(BuzhouLifecyclePhases.MEMORY);
        assertThat(lifecycle.getPhase()).isLessThan(BuzhouLifecyclePhases.CORE);
        assertThat(lifecycle.managedResourceCount()).isEqualTo(1);

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();
        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
    }

    /** {@code buzhou.memory} 子树（sleep-time 配置段）。 */
    private static Map<String, Object> memoryConfig(Map<String, Object> sleepTime) {
        return Map.of("memory", (Object) Map.of("sleep-time", sleepTime));
    }

    private static ChatModel summaryModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("九段摘要"))));
            }
        };
    }
}
