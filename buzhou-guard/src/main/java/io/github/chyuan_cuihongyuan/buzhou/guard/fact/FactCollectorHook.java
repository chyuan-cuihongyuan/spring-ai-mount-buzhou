package io.github.chyuan_cuihongyuan.buzhou.guard.fact;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;

import java.util.List;
import java.util.Optional;

/**
 * FactCollector 采集 Hook（spec 07 afterTool, order 200）。
 *
 * <p>遍历注册的 {@link FactDefinition}，对每个 {@code judge(ctx)} 命中的事实 →
 * {@link FactStore#save}（带 ttl）。判定器从入参判定语义，非硬匹配工具名。
 */
public class FactCollectorHook implements BuzhouHook {

    private final List<FactDefinition> definitions;
    private final FactStore factStore;
    /** impl-773 / spec 1020：采集计数（judge/save 隔离——单定义失败不炸链）。 */
    private final java.util.concurrent.atomic.AtomicLong saved =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong failures =
            new java.util.concurrent.atomic.AtomicLong();

    public FactCollectorHook(List<FactDefinition> definitions, FactStore factStore) {
        this.definitions = definitions == null ? List.of() : definitions;
        this.factStore = factStore;
    }

    @Override
    public String name() {
        return "FactCollectorHook";
    }

    @Override
    public int order() {
        return 200; // spec 07：afterTool 序「Spill(100) → FactCollector(200)」
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        for (FactDefinition def : definitions) {
            Optional<Fact> judged;
            try {
                judged = def.judge(ctx);
            } catch (RuntimeException e) {
                // spec 1020：judge 隔离——单定义失败不炸链（监听器隔离惯例对齐）
                failures.incrementAndGet();
                continue;
            }
            if (judged.isEmpty()) {
                continue;
            }
            try {
                Fact raw = judged.get();
                // 用 FactDefinition 的 ttl/name 补全事实元数据
                Fact fact = new Fact(
                        Fact.keyFor(def.name(), raw.key()),
                        raw.value(),
                        def.name(),
                        ctx.turn(),
                        def.ttl());
                factStore.save(ctx.sessionId(), fact);
                saved.incrementAndGet();
            } catch (RuntimeException e) {
                failures.incrementAndGet(); // save 侧失败同桶（隔离不传播）
            }
        }
        return HookResult.CONTINUE;
    }

    /** 采集只读快照（spec 1020）。 */
    public FactCollectionStats stats() {
        return new FactCollectionStats(saved.get(), failures.get());
    }

    /** 采集计数行（不可变）。 */
    public record FactCollectionStats(long saved, long failures) {
    }
}
