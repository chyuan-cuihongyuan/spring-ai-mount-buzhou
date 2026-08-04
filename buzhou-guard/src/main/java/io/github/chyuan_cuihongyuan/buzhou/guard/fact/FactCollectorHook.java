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
            Optional<Fact> judged = def.judge(ctx);
            if (judged.isPresent()) {
                Fact raw = judged.get();
                // 用 FactDefinition 的 ttl/name 补全事实元数据
                Fact fact = new Fact(
                        Fact.keyFor(def.name(), raw.key()),
                        raw.value(),
                        def.name(),
                        ctx.turn(),
                        def.ttl());
                factStore.save(ctx.sessionId(), fact);
            }
        }
        return HookResult.CONTINUE;
    }
}
